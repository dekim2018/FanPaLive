package com.livebroadcast.common.security;

import java.time.Instant;

/**
 * 인증된 요청의 사용자 정보. JWT 의 sub(userId), nickname, role, jat(가입 시각) 클레임에서 만든다.
 *
 * @param joinedAt 가입 시각. 이전 버전 토큰처럼 클레임이 없으면 null
 */
public record AuthUser(Long id, String nickname, String role, Instant joinedAt) {

    public AuthUser {
        role = role == null || role.isBlank() ? JwtClaims.ROLE_USER : role;
    }

    public AuthUser(Long id, String nickname) {
        this(id, nickname, JwtClaims.ROLE_USER, null);
    }

    public boolean isAdmin() {
        return JwtClaims.ROLE_ADMIN.equals(role);
    }
}
