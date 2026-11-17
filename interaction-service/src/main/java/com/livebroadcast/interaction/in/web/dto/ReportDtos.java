package com.livebroadcast.interaction.in.web.dto;

import java.time.Instant;
import java.util.List;

import com.livebroadcast.interaction.app.port.in.ReportUseCase.ReportView;
import com.livebroadcast.interaction.app.service.StreamerRankingService.StreamerRanking;
import com.livebroadcast.interaction.domain.ranking.StreamerRank;
import com.livebroadcast.interaction.domain.report.ModerationAction;
import com.livebroadcast.interaction.domain.report.ReportReason;
import com.livebroadcast.interaction.domain.report.ReportType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 신고, 운영 조치, 스트리머 순위 요청/응답 */
public final class ReportDtos {

    private ReportDtos() {
    }

    public record ReportRequest(@NotNull ReportType type, @NotNull Long targetId, @NotNull ReportReason reason,
                                @Size(max = 200) String detail) {
    }

    public record ReportCreated(Long id, String status) {
    }

    public record ReportResponse(Long id, String type, Long targetId, Long broadcastId, Long reporterId,
                                 String reporterNickname, String reason, String reasonLabel, String detail,
                                 String snapshot, Long subjectUserId, String status, long openCount,
                                 Instant createdAt, Instant resolvedAt) {
        public static ReportResponse from(ReportView v) {
            var r = v.report();
            return new ReportResponse(r.id(), r.type().name(), r.refId(), r.broadcastId(), r.reporterId(),
                    r.reporterNickname(), r.reason().name(), r.reason().label(), r.detail(), r.snapshot(),
                    r.subjectUserId(), r.status().name(), v.openCount(), r.createdAt(), r.resolvedAt());
        }
    }

    public record ResolveRequest(@NotNull ModerationAction action, @Size(max = 100) String note) {
    }

    public record EndBroadcastRequest(@Size(max = 100) String reason) {
    }

    public record StreamerRankingResponse(Long targetId, String month, List<Row> rows) {
        public static StreamerRankingResponse from(StreamerRanking r) {
            return new StreamerRankingResponse(r.targetId(), r.month(), r.rows().stream().map(Row::from).toList());
        }

        public record Row(int rank, Long streamerId, String nickname, long uniqueViewers, long donationAmount,
                          long score) {
            static Row from(StreamerRank s) {
                return new Row(s.rank(), s.streamerId(), s.nickname(), s.uniqueViewers(), s.donationAmount(),
                        s.score());
            }
        }
    }
}
