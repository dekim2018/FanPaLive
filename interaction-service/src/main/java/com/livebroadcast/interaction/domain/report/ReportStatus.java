package com.livebroadcast.interaction.domain.report;

public enum ReportStatus {
    OPEN,
    /** 조치함(댓글 숨김, 방송 중단) */
    ACTIONED,
    /** 문제없음으로 닫음 */
    DISMISSED
}
