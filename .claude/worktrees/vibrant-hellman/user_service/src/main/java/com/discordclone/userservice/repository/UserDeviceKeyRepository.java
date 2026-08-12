package com.discordclone.userservice.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.discordclone.userservice.entity.UserDeviceKey;

@Repository
public interface UserDeviceKeyRepository extends JpaRepository<UserDeviceKey, UUID> {
    Optional<UserDeviceKey> findByUserIdAndDeviceId(UUID userId, String deviceId);
    List<UserDeviceKey> findByPushTokenAndRevokedAtIsNull(String pushToken);
    List<UserDeviceKey> findByUserIdAndRevokedAtIsNull(UUID userId);
    List<UserDeviceKey> findByUserIdInAndRevokedAtIsNull(List<UUID> userIds);
}
