package com.discordclone.mediaservice.repository;

import com.discordclone.mediaservice.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaFileRepository extends JpaRepository<MediaFile, String> {
}
