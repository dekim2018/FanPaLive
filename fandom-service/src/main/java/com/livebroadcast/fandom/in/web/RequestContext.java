package com.livebroadcast.fandom.in.web;

import jakarta.servlet.http.HttpServletRequest;

/** 어뷰징 판단용 요청 정보. Caddy 와 Gateway 를 거치므로 실제 IP 는 X-Forwarded-For 의 첫 값이다. */
final class RequestContext {

    static final String DEVICE_HEADER = "X-Device-Id";

    private RequestContext() {
    }

    static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].strip();
        }
        return request.getRemoteAddr();
    }

    static String deviceId(HttpServletRequest request) {
        String id = request.getHeader(DEVICE_HEADER);
        if (id == null || id.length() > 64 || !id.matches("^[A-Za-z0-9-]{8,64}$")) {
            return null;
        }
        return id;
    }
}
