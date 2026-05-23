package com.discordclone.userservice.repository;

import com.discordclone.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
