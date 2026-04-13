package com.discordclone.serverservice.repository;

import com.discordclone.serverservice.entity.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServerRepository extends JpaRepository<Server, Long> {

    // Tìm tất cả server mà user là thành viên
    List<Server> findByMembersUserId(Long userId);
}
