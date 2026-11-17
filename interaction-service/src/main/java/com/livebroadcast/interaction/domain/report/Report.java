package com.livebroadcast.interaction.domain.report;

import java.time.Instant;

import com.livebroadcast.common.error.DomainException;

/**
 * 신고 한 건. 같은 사람이 같은 대상을 두 번 신고할 수 없다(DB 유일 제약).
 * 신고 시점의 내용(댓글 본문, 방송 제목)을 snapshot 으로 남겨서, 대상이 지워진 뒤에도 관리자가 판단할 수 있다.
 */
public record Report(Long id, ReportType type, Long refId, Long broadcastId, Long reporterId, String reporterNickname,
                     ReportReason reason, String detail, String snapshot, Long subjectUserId, ReportStatus status,
                     Instant createdAt, Instant resolvedAt, Long resolvedBy) {

    public static final int MAX_DETAIL = 200;
    public static final int MAX_SNAPSHOT = 300;

    public static Report open(ReportType type, Long refId, Long broadcastId, Long reporterId, String reporterNickname,
                              ReportReason reason, String detail, String snapshot, Long subjectUserId, Instant now) {
        if (type == null || refId == null || reason == null) {
            throw DomainException.badRequest("INVALID_REPORT", "신고 대상과 사유를 골라 주세요.");
        }
        if (reporterId != null && reporterId.equals(subjectUserId)) {
            throw DomainException.badRequest("SELF_REPORT", "내 방송이나 댓글은 신고할 수 없습니다.");
        }
        return new Report(null, type, refId, broadcastId, reporterId, reporterNickname, reason, cut(detail, MAX_DETAIL),
                cut(snapshot, MAX_SNAPSHOT), subjectUserId, ReportStatus.OPEN, now, null, null);
    }

    private static String cut(String value, int max) {
        if (value == null) {
            return "";
        }
        String s = value.strip();
        return s.length() > max ? s.substring(0, max) : s;
    }
}
