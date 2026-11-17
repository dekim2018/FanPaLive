package com.livebroadcast.common.security;

public final class JwtClaims {

    public static final String NICKNAME = "nickname";
    public static final String TOKEN_TYPE = "typ";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";
    /** USER, ADMIN */
    public static final String ROLE = "role";
    /** 가입 시각(epoch seconds). 랭킹 어뷰징 판단(신규 계정 가중치)에 쓴다. */
    public static final String JOINED_AT = "jat";

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    private JwtClaims() {
    }
}
