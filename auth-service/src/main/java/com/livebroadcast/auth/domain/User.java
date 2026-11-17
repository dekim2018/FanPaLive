package com.livebroadcast.auth.domain;

import java.time.Instant;
import java.util.regex.Pattern;

import com.livebroadcast.common.error.DomainException;

/**
 * 회원 도메인 모델. JPA 엔티티와 분리된 순수 자바 객체다.
 */
public class User {

    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9_]{4,20}$");

    private final Long id;
    private final String username;
    private final String passwordHash;
    private final String nickname;
    private final Role role;
    private final Instant createdAt;

    public User(Long id, String username, String passwordHash, String nickname, Role role, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.role = role == null ? Role.USER : role;
        this.createdAt = createdAt;
    }

    public User(Long id, String username, String passwordHash, String nickname, Instant createdAt) {
        this(id, username, passwordHash, nickname, Role.USER, createdAt);
    }

    public static User register(String username, String passwordHash, String nickname, Instant now) {
        if (username == null || !USERNAME.matcher(username).matches()) {
            throw DomainException.badRequest("INVALID_USERNAME", "아이디는 영문 소문자, 숫자, _ 조합 4~20자여야 합니다.");
        }
        if (nickname == null || nickname.isBlank() || nickname.strip().length() > 20) {
            throw DomainException.badRequest("INVALID_NICKNAME", "닉네임은 1~20자여야 합니다.");
        }
        return new User(null, username, passwordHash, nickname.strip(), Role.USER, now);
    }

    /** 비밀번호만 바꾼 회원 */
    public User withPasswordHash(String newPasswordHash) {
        return new User(id, username, newPasswordHash, nickname, role, createdAt);
    }

    /** 역할만 바꾼 회원 (평가용 관리자 계정 준비에 사용) */
    public User withRole(Role newRole) {
        return new User(id, username, passwordHash, nickname, newRole, createdAt);
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public Role getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
