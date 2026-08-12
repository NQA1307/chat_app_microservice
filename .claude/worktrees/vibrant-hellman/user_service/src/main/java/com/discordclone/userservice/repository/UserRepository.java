package com.discordclone.userservice.repository;

import com.discordclone.userservice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, java.util.UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    List<User> findByIdIn(Collection<java.util.UUID> ids);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Query("""
        SELECT u FROM User u
        WHERE (:q IS NULL OR :q = ''
           OR LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')))
    """)
    Page<User> searchAdminUsers(@Param("q") String q, Pageable pageable);

    long countByEmailVerifiedTrue();

    long countByBannedTrue();

    long countByLockedTrue();

    long countByCreatedAtAfter(LocalDateTime from);
}
