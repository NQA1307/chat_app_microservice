package com.discordclone.mediaservice.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.discordclone.mediaservice.client.ServerServiceClient;
import com.discordclone.mediaservice.entity.MediaAsset;
import com.discordclone.mediaservice.entity.MediaFile;
import com.discordclone.mediaservice.repository.MediaAssetRepository;
import com.discordclone.mediaservice.repository.MediaFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MediaServiceTest {

    private MediaFileRepository mediaFileRepository;
    private MediaAssetRepository mediaAssetRepository;
    private Uploader uploader;
    private MediaService mediaService;

    @BeforeEach
    void setUp() {
        mediaFileRepository = mock(MediaFileRepository.class);
        mediaAssetRepository = mock(MediaAssetRepository.class);
        Cloudinary cloudinary = mock(Cloudinary.class);
        uploader = mock(Uploader.class);
        ServerServiceClient serverServiceClient = mock(ServerServiceClient.class);

        when(cloudinary.uploader()).thenReturn(uploader);
        when(mediaFileRepository.save(any(MediaFile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mediaAssetRepository.save(any(MediaAsset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mediaService = new MediaService(mediaFileRepository, mediaAssetRepository, cloudinary, serverServiceClient);
    }

    @Test
    void uploadAttachmentReusesExistingAssetInsteadOfUploadingAgain() throws Exception {
        MockMultipartFile file = textFile();
        MediaAsset existingAsset = cloudinaryAsset(file.getSize());

        when(mediaAssetRepository.findFirstByCheckSumAndSize(anyString(), eq(file.getSize())))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingAsset));
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
                "secure_url", existingAsset.getPublicUrl(),
                "public_id", existingAsset.getStorageKey()
        ));

        mediaService.uploadAttachment(file, UUID.randomUUID(), null);
        mediaService.uploadAttachment(file, UUID.randomUUID(), null);

        verify(uploader).upload(any(), anyMap());
    }

    @Test
    void uploadAttachmentUsesChecksumAsCloudinaryPublicId() throws Exception {
        MockMultipartFile file = textFile();
        MediaAsset asset = cloudinaryAsset(file.getSize());

        when(mediaAssetRepository.findFirstByCheckSumAndSize(anyString(), eq(file.getSize())))
                .thenReturn(Optional.empty());
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
                "secure_url", asset.getPublicUrl(),
                "public_id", asset.getStorageKey()
        ));

        mediaService.uploadAttachment(file, UUID.randomUUID(), null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> optionsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(), optionsCaptor.capture());

        Map<String, Object> options = optionsCaptor.getValue();
        assertThat(options.get("public_id").toString()).startsWith("rediscord/attachments/");
        assertThat(options).containsEntry("overwrite", false);
        assertThat(options).containsEntry("unique_filename", false);
        assertThat(options).containsEntry("resource_type", "raw");
    }

    @Test
    void deleteMediaKeepsCloudinaryAssetWhenOtherFilesStillReferenceIt() throws Exception {
        MediaAsset asset = cloudinaryAsset(11);
        MediaFile mediaFile = mediaFile(asset);

        when(mediaFileRepository.findById(mediaFile.getId())).thenReturn(Optional.of(mediaFile));
        when(mediaFileRepository.countByAssetId(asset.getId())).thenReturn(1L);

        mediaService.deleteMedia(mediaFile.getId(), mediaFile.getOwnerId());

        verify(uploader, never()).destroy(anyString(), anyMap());
    }

    @Test
    void deleteMediaDestroysCloudinaryAssetWhenLastReferenceIsRemoved() throws Exception {
        MediaAsset asset = cloudinaryAsset(11);
        MediaFile mediaFile = mediaFile(asset);

        when(mediaFileRepository.findById(mediaFile.getId())).thenReturn(Optional.of(mediaFile));
        when(mediaFileRepository.countByAssetId(asset.getId())).thenReturn(0L);

        mediaService.deleteMedia(mediaFile.getId(), mediaFile.getOwnerId());

        verify(uploader).destroy(eq(asset.getStorageKey()), anyMap());
        verify(mediaAssetRepository).delete(asset);
    }

    private MockMultipartFile textFile() {
        return new MockMultipartFile(
                "file",
                "note.txt",
                "text/plain",
                "hello world".getBytes()
        );
    }

    private MediaAsset cloudinaryAsset(long size) {
        return MediaAsset.builder()
                .id("asset-1")
                .checkSum("checksum")
                .size(size)
                .contentType("text/plain")
                .storageProvider("cloudinary")
                .storageKey("rediscord/attachments/checksum")
                .publicUrl("https://res.cloudinary.com/demo/raw/upload/rediscord/attachments/checksum")
                .resourceType("raw")
                .build();
    }

    private MediaFile mediaFile(MediaAsset asset) {
        return MediaFile.builder()
                .id("file-1")
                .ownerId(UUID.randomUUID())
                .originalFilename("note.txt")
                .contentType(asset.getContentType())
                .size(asset.getSize())
                .checkSum(asset.getCheckSum())
                .storageProvider(asset.getStorageProvider())
                .storageKey(asset.getStorageKey())
                .storagePath(asset.getStorageKey())
                .publicUrl(asset.getPublicUrl())
                .asset(asset)
                .build();
    }
}
