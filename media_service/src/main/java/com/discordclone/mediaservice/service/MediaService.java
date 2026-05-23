package com.discordclone.mediaservice.service;

import com.discordclone.common.exception.AppException;
import com.discordclone.mediaservice.dto.MediaUploadResponse;
import com.discordclone.mediaservice.entity.MediaFile;
import com.discordclone.mediaservice.repository.MediaFileRepository;
import de.huxhorn.sulky.ulid.ULID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final MediaFileRepository mediaFileRepository;
    private final ULID ulid = new ULID();

    @Value("${app.media.upload-dir}")
    private String uploadDir;

    @Value("${app.media.public-base-url}")
    private String publicBaseUrl;

    @Transactional
    public MediaUploadResponse uploadImage(MultipartFile file, UUID ownerId) {
        validateImage(file);

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
                .storagePath(targetPath.toString())
                .publicUrl(publicUrl)
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

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "File is required");
        }

        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Only jpeg, png, webp and gif images are allowed");
        }
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

    private MediaUploadResponse toResponse(MediaFile mediaFile) {
        return MediaUploadResponse.builder()
                .id(mediaFile.getId())
                .originalFilename(mediaFile.getOriginalFilename())
                .contentType(mediaFile.getContentType())
                .size(mediaFile.getSize())
                .url(mediaFile.getPublicUrl())
                .build();
    }
}
