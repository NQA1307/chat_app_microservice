package com.discordclone.userservice.service;

import com.discordclone.common.exception.AppException;
import com.discordclone.userservice.dto.LoginRequest;
import com.discordclone.userservice.dto.RegisterRequest;
import com.discordclone.userservice.dto.UserResponse;
import com.discordclone.userservice.entity.User;
import com.discordclone.userservice.repository.UserRepository;
import com.discordclone.userservice.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // ── Register ───────────────────────────────────────────────
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(HttpStatus.CONFLICT, "Email already in use");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(HttpStatus.CONFLICT, "Username already taken");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName())
                .role(User.Role.USER)
                .build();

        return toResponse(userRepository.save(user));
    }

    // ── Login ──────────────────────────────────────────────────
    public String login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    // ── Get User ───────────────────────────────────────────────
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));
        return toResponse(user);
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));
        return toResponse(user);
    }

    // ── Mapper ─────────────────────────────────────────────────
    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
