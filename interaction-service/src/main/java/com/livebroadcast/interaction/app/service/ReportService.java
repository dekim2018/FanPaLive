package com.livebroadcast.interaction.app.service;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livebroadcast.common.error.DomainException;
import com.livebroadcast.interaction.app.PageResult;
import com.livebroadcast.interaction.app.port.in.ReportUseCase;
import com.livebroadcast.interaction.app.port.out.BroadcastModerationPort;
import com.livebroadcast.interaction.app.port.out.BroadcastQueryPort;
import com.livebroadcast.interaction.app.port.out.CommentRepositoryPort;
import com.livebroadcast.interaction.app.port.out.DomainEventPublisherPort;
import com.livebroadcast.interaction.app.port.out.ReportRepositoryPort;
import com.livebroadcast.interaction.domain.broadcast.BroadcastInfo;
import com.livebroadcast.interaction.domain.comment.Comment;
import com.livebroadcast.interaction.domain.comment.CommentDeleted;
import com.livebroadcast.interaction.domain.report.ModerationAction;
import com.livebroadcast.interaction.domain.report.Report;
import com.livebroadcast.interaction.domain.report.ReportReason;
import com.livebroadcast.interaction.domain.report.ReportStatus;
import com.livebroadcast.interaction.domain.report.ReportType;

/**
 * 신고와 운영 제재.
 * 신고는 누구나(로그인) 하고, 처리는 관리자만 한다. 같은 대상 신고가 여러 건이면 한 번 처리로 함께 닫는다.
 * 댓글 숨김은 댓글을 지우고 실시간으로 화면에서 내리며(comment.deleted), 내용은 신고 기록(snapshot)에 남는다.
 * 방송 중단은 broadcast-service 가 방송을 종료하고 공개 목록에서 숨긴 뒤 다시 시작하지 못하게 한다.
 */
@Service
public class ReportService implements ReportUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final ReportRepositoryPort reports;
    private final CommentRepositoryPort comments;
    private final BroadcastQueryPort broadcasts;
    private final BroadcastModerationPort moderation;
    private final DomainEventPublisherPort events;
    private final Clock clock;

    public ReportService(ReportRepositoryPort reports, CommentRepositoryPort comments, BroadcastQueryPort broadcasts,
                         BroadcastModerationPort moderation, DomainEventPublisherPort events, Clock clock) {
        this.reports = reports;
        this.comments = comments;
        this.broadcasts = broadcasts;
        this.moderation = moderation;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Report report(Long reporterId, String reporterNickname, ReportType type, Long refId, ReportReason reason,
                         String detail) {
        if (type == null || refId == null) {
            throw DomainException.badRequest("INVALID_REPORT", "신고 대상과 사유를 골라 주세요.");
        }
        Instant now = clock.instant();
        Report report = switch (type) {
            case COMMENT -> {
                Comment c = comments.findById(refId).orElseThrow(ReportService::targetNotFound);
                yield Report.open(type, refId, c.getBroadcastId(), reporterId, reporterNickname, reason, detail,
                        c.getAuthorNickname() + ": " + c.getContent(), c.getAuthorId(), now);
            }
            case BROADCAST -> {
                BroadcastInfo b = broadcasts.findBroadcast(refId).orElseThrow(ReportService::targetNotFound);
                yield Report.open(type, refId, refId, reporterId, reporterNickname, reason, detail,
                        b.ownerNickname() + ": " + b.title(), b.ownerId(), now);
            }
        };
        return reports.insert(report).orElseThrow(() -> DomainException.conflict("ALREADY_REPORTED",
                "이미 신고한 대상입니다. 운영자가 확인할 때까지 기다려 주세요."));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ReportView> list(ReportStatus status, int page, int size) {
        int safe = size <= 0 ? 20 : Math.min(size, 50);
        return reports.findByStatus(status == null ? ReportStatus.OPEN : status, Math.max(page, 0), safe)
                .map(r -> new ReportView(r, r.status() == ReportStatus.OPEN ? reports.countOpen(r.type(), r.refId())
                        : 0));
    }

    @Override
    @Transactional
    public int resolve(Long adminId, Long reportId, ModerationAction action, String note) {
        Report report = reports.findById(reportId)
                .orElseThrow(() -> DomainException.notFound("REPORT_NOT_FOUND", "신고를 찾을 수 없습니다."));
        if (action == null || !action.appliesTo(report.type())) {
            throw DomainException.badRequest("INVALID_ACTION", "이 신고에 쓸 수 없는 조치입니다.");
        }
        switch (action) {
            case HIDE_COMMENT -> removeComment(report.refId());
            case END_BROADCAST -> moderation.moderate(report.refId(), reasonText(report.reason(), note));
            case DISMISS -> {
            }
        }
        ReportStatus status = action == ModerationAction.DISMISS ? ReportStatus.DISMISSED : ReportStatus.ACTIONED;
        int closed = reports.resolveAll(report.type(), report.refId(), status, adminId, clock.instant());
        log.info("신고 처리 adminId={} type={} refId={} action={} 닫은 신고={}", adminId, report.type(), report.refId(),
                action, closed);
        return closed;
    }

    @Override
    @Transactional
    public void hideComment(Long adminId, Long commentId) {
        removeComment(commentId);
        reports.resolveAll(ReportType.COMMENT, commentId, ReportStatus.ACTIONED, adminId, clock.instant());
    }

    @Override
    @Transactional
    public boolean endBroadcast(Long adminId, Long broadcastId, String reason) {
        boolean ended = moderation.moderate(broadcastId, reason == null || reason.isBlank() ? "운영 정책 위반" : reason);
        reports.resolveAll(ReportType.BROADCAST, broadcastId, ReportStatus.ACTIONED, adminId, clock.instant());
        return ended;
    }

    private void removeComment(Long commentId) {
        // 이미 작성자가 지웠으면 할 일이 없다(신고 기록에는 내용이 남아 있다).
        comments.findById(commentId).ifPresent(c -> {
            comments.delete(c.getId());
            events.publish(new CommentDeleted(c.getId(), c.getBroadcastId()));
        });
    }

    private static String reasonText(ReportReason reason, String note) {
        String base = reason == null ? "운영 정책 위반" : reason.label();
        return note == null || note.isBlank() ? base : base + " (" + note.strip() + ")";
    }

    private static DomainException targetNotFound() {
        return DomainException.notFound("REPORT_TARGET_NOT_FOUND", "신고할 방송이나 댓글을 찾을 수 없습니다.");
    }
}
