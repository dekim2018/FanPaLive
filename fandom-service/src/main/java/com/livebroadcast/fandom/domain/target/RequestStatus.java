package com.livebroadcast.fandom.domain.target;

/**
 * 팀/선수 등록 신청 상태.
 *
 * <pre>
 * PENDING -> APPROVED (관리자 승인, 대상 생성)
 * PENDING -> REJECTED (관리자 반려, 사유 필수)
 * </pre>
 */
public enum RequestStatus {
    PENDING,
    APPROVED,
    REJECTED;

    public boolean canTransitionTo(RequestStatus next) {
        return this == PENDING && (next == APPROVED || next == REJECTED);
    }

    public boolean isOpen() {
        return this == PENDING;
    }
}
