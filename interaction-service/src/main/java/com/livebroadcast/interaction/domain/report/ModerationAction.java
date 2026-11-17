package com.livebroadcast.interaction.domain.report;

/** 관리자가 신고를 처리하는 방법. 대상 종류마다 쓸 수 있는 조치가 다르다. */
public enum ModerationAction {
    HIDE_COMMENT(ReportType.COMMENT),
    END_BROADCAST(ReportType.BROADCAST),
    DISMISS(null);

    private final ReportType appliesTo;

    ModerationAction(ReportType appliesTo) {
        this.appliesTo = appliesTo;
    }

    public boolean appliesTo(ReportType type) {
        return appliesTo == null || appliesTo == type;
    }
}
