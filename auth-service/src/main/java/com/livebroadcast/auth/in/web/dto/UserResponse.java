package com.livebroadcast.auth.in.web.dto;

import java.time.Instant;

import com.livebroadcast.auth.domain.User;

public record UserResponse(Long id, String username, String nickname, String role, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getNickname(), user.getRole().name(),
                user.getCreatedAt());
    }
}
