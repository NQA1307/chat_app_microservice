package com.discordclone.mediaservice.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.discordclone.common.exception.AppException;
import com.discordclone.mediaservice.client.ServerServiceClient;
import com.discordclone.mediaservice.dto.MediaUploadResponse;
import com.discordclone.mediaservice.entity.MediaAsset;
import com.discordclone.mediaservice.entity.MediaFile;
import com.discordclone.mediaservice.repository.MediaAssetRepository;
import com.discordclone.mediaservice.repository.MediaFileRepository;
import de.huxhorn.sulky.ulid.ULID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.apache.tika.Tika;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaService {

    // Image uploads are used for avatars/server icons, so keep the accepted types narrow.
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    // Chat attachments can accept images, videos, and a small set of safe document/archive types.
    private static final Set<String> ALLOWED_ATTACHMENT_TYPES = Set.of(
        "image/jpeg",
        "image/png",
        "image/webp",
        "image/gif",
        "video/mp4",
        "video/webm",
        "video/quicktime",
        "application/pdf",
        "text/plain",
        "application/zip",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    // Block executable/script-like extensions even if the browser sends a misleading MIME type.
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            ".exe",
            ".bat",
            ".cmd",
            ".sh",
            ".js",
            ".html",
            ".htm",
            ".php",
            ".jar"
    );
    

    // Keep avatar/icon uploads small; attachments are larger, and videos get their own limit.
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final long MAX_ATTACHMENT_SIZE = 50 * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE = 50 * 1024 * 1024;

    private final MediaFileRepository mediaFileRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final Tika tika = new Tika();
    private final Cloudinary cloudinary;
    private final ULID ulid = new ULID();
    private final ServerServiceClient serverServiceClient;

    @Value("${app.media.upload-dir}")
    private String uploadDir;

    @Value("${app.media.public-base-url}")
    private String publicBaseUrl;

    @Transactional
    public MediaUploadResponse uploadImage(MultipartFile file, UUID ownerId) {
        validateImage(file);

        String checkSum = calculateSha256(file);

        Optional<MediaFile> existingFile = mediaFileRepository.findFirstByOwnerIdAndCheckSumAndSize(
            ownerId, 
            checkSum, 
            file.getSize()
        );

        if (existingFile.isPresent()) {
            return toResponse(existingFile.get());
        }

        String id = ulid.nextULID();
        String extension = extensionFor(file.getContentType());
        Path targetPath = Path.of(uploadDir, id + extension).toAbsolutePath().normalize();

        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath);
        } catch (IOException ex) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store uploaded file");
        }

        String publicUrl = publicBaseUrl + "/api/media/files/" + id;
        MediaFile mediaFile = MediaFile.builder()
                .id(id)
                .ownerId(ownerId)
                .originalFilename(file.getOriginalFilename() == null ? id + extension : file.getOriginalFilename())
                .contentType(file.getContentType())
                .size(file.getSize())
                .checkSum(checkSum)
                .storageProvider("local")
                .storageKey(targetPath.toString())
                .storagePath(targetPath.toString())
                .publicUrl(publicUrl)
                .build();

        return toResponse(mediaFileRepository.save(mediaFile));
    }

    //Upload file
    @Transactional
    public MediaUploadResponse uploadAttachment(MultipartFile file, UUID ownerId, Long channelId) {

        if (channelId != null) {
            serverServiceClient.checkChannelAccess(channelId, ownerId);
        }
        
        String detectedType = validateAttachment(file);

        String checkSum = calculateSha256(file);

        MediaAsset asset = findOrCreateCloudinaryAsset(file, checkSum, detectedType);
        MediaFile mediaFile = MediaFile.builder()
                        .id(ulid.nextULID())
                        .ownerId(ownerId)
                        .originalFilename(sanitizeFilename(file.getOriginalFilename()))
                        .contentType(detectedType)
                        .size(file.getSize())
                        .checkSum(checkSum)
                        .publicUrl(asset.getPublicUrl())
                        .storagePath(asset.getStorageKey())
                        .storageProvider(asset.getStorageProvider())
                        .storageKey(asset.getStorageKey())
                        .asset(asset)
                        .build();

        return toResponse(mediaFileRepository.save(mediaFile));
    }

    public MediaFile getMediaFile(String id) {
        return mediaFileRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Media file not found"));
    }

    public Resource getFileResource(String id) {
        MediaFile mediaFile = getMediaFile(id);
        Resource resource = new FileSystemResource(mediaFile.getStoragePath());
        if (!resource.exists()) {
            throw new AppException(HttpStatus.NOT_FOUND, "Stored file not found");
        }
        return resource;
    }

    @Transactional
    public void deleteMedia(String id, UUID ownerId) {
        MediaFile mediaFile = getMediaFile(id);
        if (!Objects.equals(mediaFile.getOwnerId(), ownerId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "You cannot delete this media file");
        }

        mediaFileRepository.delete(mediaFile);
        deleteStoredFileIfUnused(mediaFile);
    }

    private void deleteStoredFileIfUnused(MediaFile mediaFile) {
        MediaAsset asset = mediaFile.getAsset();
        if (asset != null) {
            if (mediaFileRepository.countByAssetId(asset.getId()) > 0) {
                return;
            }

            deleteCloudinaryAsset(asset.getStorageKey(), asset.getResourceType());
            mediaAssetRepository.delete(asset);
            return;
        }

        Path localPath = Path.of(mediaFile.getStoragePath());
        if (Files.exists(localPath)) {
            try {
                Files.deleteIfExists(localPath);
            } catch (IOException ex) {
                throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not delete stored file");
            }
            return;
        }

        deleteCloudinaryAsset(mediaFile.getStoragePath(), cloudinaryResourceType(mediaFile.getContentType()));
    }

    private MediaAsset findOrCreateCloudinaryAsset(MultipartFile file, String checkSum, String detectedType) {
        return mediaAssetRepository.findFirstByCheckSumAndSize(checkSum, file.getSize())
                .orElseGet(() -> uploadAndCreateCloudinaryAsset(file, checkSum, detectedType));
    }

    private MediaAsset uploadAndCreateCloudinaryAsset(MultipartFile file, String checkSum, String detectedType) {
        String resourceType = cloudinaryResourceType(detectedType);
        String publicId = "rediscord/attachments/" + checkSum;

        try {
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "resource_type", resourceType,
                            "overwrite", false,
                            "unique_filename", false
                    )
            );

            String publicUrl = (String) uploadResult.get("secure_url");
            String cloudinaryPublicId = (String) uploadResult.get("public_id");

            MediaAsset asset = MediaAsset.builder()
                    .id(ulid.nextULID())
                    .checkSum(checkSum)
                    .size(file.getSize())
                    .contentType(detectedType)
                    .storageProvider("cloudinary")
                    .storageKey(cloudinaryPublicId)
                    .publicUrl(publicUrl)
                    .resourceType(resourceType)
                    .build();

            return mediaAssetRepository.save(asset);
        } catch (DataIntegrityViolationException ex) {
            return mediaAssetRepository.findFirstByCheckSumAndSize(checkSum, file.getSize())
                    .orElseThrow(() -> new AppException(HttpStatus.CONFLICT, "Could not resolve duplicated media asset"));
        } catch (Exception ex) {
            return mediaAssetRepository.findFirstByCheckSumAndSize(checkSum, file.getSize())
                    .orElseThrow(() -> new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not upload attachment"));
        }
    }

    private void deleteCloudinaryAsset(String storageKey, String resourceType) {
        try {
            cloudinary.uploader().destroy(storageKey, ObjectUtils.asMap("resource_type", resourceType));
        } catch (IOException ex) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not delete stored file");
        }
    }

    private String cloudinaryResourceType(String contentType) {
        if (contentType != null && contentType.startsWith("image/")) {
            return "image";
        }
        if (contentType != null && contentType.startsWith("video/")) {
            return "video";
        }
        return "raw";
    }

    // Validate avatar/server icon uploads before storing them.
    private void validateImage(MultipartFile file) {
        validateRequired(file);

        String detectedType = detectContentType(file);
        if (!ALLOWED_IMAGE_TYPES.contains(detectedType)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Only jpeg, png, webp and gif images are allowed");
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Image must be smaller than 5MB");
        }

        validateDangerousExtension(file.getOriginalFilename());
    }


    // Validate chat attachments with a broader allowlist and size limits.
    private String validateAttachment(MultipartFile file) {
        validateRequired(file);

        String detectedType = detectContentType(file);

        if (!ALLOWED_ATTACHMENT_TYPES.contains(detectedType)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "This file type is not allowed");
        }

        long maxSize = detectedType.startsWith("video/") ? MAX_VIDEO_SIZE : MAX_ATTACHMENT_SIZE;
        if (file.getSize() > maxSize) {
            throw new AppException(HttpStatus.BAD_REQUEST, "File size is too large");
        }

        validateDangerousExtension(file.getOriginalFilename());

        return detectedType;
    }
    // Common required-field validation shared by image and attachment uploads.
    private void validateRequired(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "File is required");
        }

        validateFilename(file.getOriginalFilename());

        if(file.getContentType() == null || file.getContentType().isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "File content type is required");
        }
    }

    // Reject filename extensions that should never be uploaded as user content.
    private void validateDangerousExtension(String filename) {
        String lowerFilename = filename.toLowerCase();
        boolean blocked = BLOCKED_EXTENSIONS.stream().anyMatch(lowerFilename::endsWith);
        if (blocked) {
            throw new AppException(HttpStatus.BAD_REQUEST, "This file extension is not allowed");
        }
    }

    // Store only a clean basename in metadata; do not trust client-provided paths.
    private String sanitizeFilename(String filename) {
        return Path.of(filename).getFileName().toString();
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> "";
        };
    }

    //Map ve FileResponse
    private MediaUploadResponse toResponse(MediaFile mediaFile) {
        return MediaUploadResponse.builder()
                .id(mediaFile.getId())
                .originalFilename(mediaFile.getOriginalFilename())
                .contentType(mediaFile.getContentType())
                .size(mediaFile.getSize())
                .url(mediaFile.getPublicUrl())
                .build();
    }


    //Xac dinh dang content cua file
    private String detectContentType(MultipartFile file) {
        try {
            return tika.detect(file.getInputStream(), file.getOriginalFilename());
        } catch (IOException ex) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Could not detect file type");
        }
    }

    //Kiem tra ten file
    private void validateFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "File name is required");
        }

        if (filename.length() > 200) {
            throw new AppException(HttpStatus.BAD_REQUEST, "File name is too long");
        }

        if (filename.contains("..") || filename.contains("/") || filename.contains("\\") || filename.contains("\0")) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }

        validateDangerousExtension(filename);
    }


    //tạo hash cho file tranh trùng lặp
    private String calculateSha256(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            byte[] hashBytes = digest.digest();
            StringBuilder hex = new StringBuilder();

            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();
        } catch (IOException ex) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Could not read uploaded file");
        } catch (NoSuchAlgorithmException ex) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not calculate file checksum");
        }
    }

}
