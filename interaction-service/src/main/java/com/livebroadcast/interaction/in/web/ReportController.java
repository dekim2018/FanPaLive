package com.livebroadcast.interaction.in.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.livebroadcast.common.error.DomainException;
import com.livebroadcast.common.security.AuthUser;
import com.livebroadcast.common.security.LoginUser;
import com.livebroadcast.interaction.app.port.in.ReportUseCase;
import com.livebroadcast.interaction.app.service.StreamerRankingService;
import com.livebroadcast.interaction.domain.report.ReportStatus;
import com.livebroadcast.interaction.in.web.dto.PageResponse;
import com.livebroadcast.interaction.in.web.dto.ReportDtos.EndBroadcastRequest;
import com.livebroadcast.interaction.in.web.dto.ReportDtos.ReportCreated;
import com.livebroadcast.interaction.in.web.dto.ReportDtos.ReportRequest;
import com.livebroadcast.interaction.in.web.dto.ReportDtos.ReportResponse;
import com.livebroadcast.interaction.in.web.dto.ReportDtos.ResolveRequest;
import com.livebroadcast.interaction.in.web.dto.ReportDtos.StreamerRankingResponse;

import jakarta.validation.Valid;

/**
 * 신고(로그인 사용자), 운영 조치(관리자), 스트리머 순위(누구나).
 * 관리자 경로를 /api/moderation 으로 둔 이유: /api/admin/** 은 게이트웨이에서 fandom-service 로 간다.
 */
@RestController
public class ReportController {

    private final ReportUseCase reports;
    private final StreamerRankingService streamerRanking;

    public ReportController(ReportUseCase reports, StreamerRankingService streamerRanking) {
        this.reports = reports;
        this.streamerRanking = streamerRanking;
    }

    @PostMapping("/api/reports")
    public ResponseEntity<ReportCreated> report(@LoginUser AuthUser user, @Valid @RequestBody ReportRequest body) {
        var r = reports.report(user.id(), user.nickname(), body.type(), body.targetId(), body.reason(),
                body.detail());
        return ResponseEntity.status(HttpStatus.CREATED).body(new ReportCreated(r.id(), r.status().name()));
    }

    @GetMapping("/api/moderation/reports")
    public PageResponse<ReportResponse> list(@LoginUser AuthUser admin,
                                             @RequestParam(defaultValue = "OPEN") ReportStatus status,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        ensureAdmin(admin);
        return PageResponse.from(reports.list(status, page, size).map(ReportResponse::from));
    }

    @PostMapping("/api/moderation/reports/{reportId}/resolve")
    public Map<String, Integer> resolve(@LoginUser AuthUser admin, @PathVariable Long reportId,
                                        @Valid @RequestBody ResolveRequest body) {
        ensureAdmin(admin);
        return Map.of("closed", reports.resolve(admin.id(), reportId, body.action(), body.note()));
    }

    @PostMapping("/api/moderation/comments/{commentId}/hide")
    public ResponseEntity<Void> hideComment(@LoginUser AuthUser admin, @PathVariable Long commentId) {
        ensureAdmin(admin);
        reports.hideComment(admin.id(), commentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/moderation/broadcasts/{broadcastId}/end")
    public Map<String, Boolean> endBroadcast(@LoginUser AuthUser admin, @PathVariable Long broadcastId,
                                             @Valid @RequestBody(required = false) EndBroadcastRequest body) {
        ensureAdmin(admin);
        return Map.of("ended", reports.endBroadcast(admin.id(), broadcastId, body == null ? null : body.reason()));
    }

    /** 응원팀별 월간 스트리머 순위. month 를 비우면 이번 달(KST) */
    @GetMapping("/api/streamers/ranking")
    public StreamerRankingResponse ranking(@RequestParam("target") Long targetId,
                                           @RequestParam(required = false) String month) {
        return StreamerRankingResponse.from(streamerRanking.ranking(targetId, month));
    }

    private static void ensureAdmin(AuthUser user) {
        if (user == null || !user.isAdmin()) {
            throw DomainException.forbidden("ADMIN_ONLY", "관리자만 할 수 있는 작업입니다.");
        }
    }
}
