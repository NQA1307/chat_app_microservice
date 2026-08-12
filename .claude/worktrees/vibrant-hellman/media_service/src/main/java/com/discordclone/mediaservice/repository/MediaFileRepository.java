package com.discordclone.mediaservice.repository;

import com.discordclone.mediaservice.entity.MediaFile;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaFileRepository extends JpaRepository<MediaFile, String> {
    Optional<MediaFile> findFirstByOwnerIdAndCheckSumAndSize(
        UUID ownerId,
        String checkSum,
        long size
    );

    Optional<MediaFile> findFirstByCheckSumAndSize(String checksum, long size);

    long countByAssetId(String assetId);
}
