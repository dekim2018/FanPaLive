package com.livebroadcast.auth.domain;

/**
 * 회원 역할. ADMIN 은 팀/선수 등록 신청 승인, 경기 등록 같은 운영 기능을 쓴다.
 * 방송 수정, 삭제 같은 소유자 권한은 역할과 무관하게 소유자만 가진다.
 */
public enum Role {
    USER,
    ADMIN;

    public static Role from(String value) {
        if (value == null || value.isBlank()) {
            return USER;
        }
        return Role.valueOf(value.strip().toUpperCase());
    }
}
