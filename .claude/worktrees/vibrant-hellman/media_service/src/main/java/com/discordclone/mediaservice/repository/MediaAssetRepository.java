package com.discordclone.mediaservice.repository;

import com.discordclone.mediaservice.entity.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, String> {
    Optional<MediaAsset> findFirstByCheckSumAndSize(String checkSum, long size);
}
