package com.livebroadcast.interaction.domain.report;

/** 신고 사유. 편파중계 특성상 중계권 영상 재송출이 가장 중요한 사유다. */
public enum ReportReason {
    RIGHTS_VIOLATION("중계권 영상 재송출"),
    ABUSE("욕설, 비하, 혐오 표현"),
    SPAM("도배, 광고"),
    ETC("기타");

    private final String label;

    ReportReason(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
