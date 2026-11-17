package com.livebroadcast.interaction.app.port.in;

import com.livebroadcast.interaction.app.PageResult;
import com.livebroadcast.interaction.domain.report.ModerationAction;
import com.livebroadcast.interaction.domain.report.Report;
import com.livebroadcast.interaction.domain.report.ReportReason;
import com.livebroadcast.interaction.domain.report.ReportStatus;
import com.livebroadcast.interaction.domain.report.ReportType;

public interface ReportUseCase {

    Report report(Long reporterId, String reporterNickname, ReportType type, Long refId, ReportReason reason,
                  String detail);

    PageResult<ReportView> list(ReportStatus status, int page, int size);

    /** 신고를 처리한다. 같은 대상의 열린 신고가 모두 함께 닫힌다. 닫은 건수 */
    int resolve(Long adminId, Long reportId, ModerationAction action, String note);

    /** 신고 없이 바로 조치(관리자가 직접 본 경우) */
    void hideComment(Long adminId, Long commentId);

    boolean endBroadcast(Long adminId, Long broadcastId, String reason);

    record ReportView(Report report, long openCount) {
    }
}
