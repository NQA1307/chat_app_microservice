package com.discordclone.mediaservice.controller;

import com.discordclone.common.dto.ApiResponse;
import com.discordclone.mediaservice.dto.MediaUploadResponse;
import com.discordclone.mediaservice.entity.MediaFile;
import com.discordclone.mediaservice.service.MediaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    private UUID getUserId(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId == null) {
            throw new IllegalArgumentException("Missing X-User-Id header");
        }
        return UUID.fromString(userId);
    }

    //Upload anh
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MediaUploadResponse>> uploadImage(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) {
        MediaUploadResponse response = mediaService.uploadImage(file, getUserId(request));
        return ResponseEntity.ok(ApiResponse.success("Media uploaded", response));
    }
    
    //Lay file
    @GetMapping("/files/{id}")
    public ResponseEntity<Resource> getFile(@PathVariable("id") String id) {
        MediaFile mediaFile = mediaService.getMediaFile(id);
        Resource resource = mediaService.getFileResource(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mediaFile.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + mediaFile.getOriginalFilename() + "\"")
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .body(resource);
    }


    //Upload file
    @PostMapping(value = "/upload/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MediaUploadResponse>> uploadAttachment(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "channelId", required = false) Long channelId,
            HttpServletRequest request
    ) {
        MediaUploadResponse response = mediaService.uploadAttachment(
                file,
                getUserId(request),
                channelId
        );

        return ResponseEntity.ok(ApiResponse.success("Attachment uploaded", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(
            @PathVariable("id") String id,
            HttpServletRequest request
    ) {
        mediaService.deleteMedia(id, getUserId(request));
        return ResponseEntity.ok(ApiResponse.success("Media deleted", null));
    }

}
