package com.livebroadcast.interaction.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.livebroadcast.interaction.app.PageResult;
import com.livebroadcast.interaction.app.port.out.CommentRepositoryPort;
import com.livebroadcast.interaction.app.port.out.ReportRepositoryPort;
import com.livebroadcast.interaction.domain.broadcast.BroadcastInfo;
import com.livebroadcast.interaction.domain.comment.Comment;
import com.livebroadcast.interaction.domain.comment.CommentDeleted;
import com.livebroadcast.interaction.domain.report.ModerationAction;
import com.livebroadcast.interaction.domain.report.Report;
import com.livebroadcast.interaction.domain.report.ReportReason;
import com.livebroadcast.interaction.domain.report.ReportStatus;
import com.livebroadcast.interaction.domain.report.ReportType;

class ReportServiceTest {

    static final Instant NOW = Instant.parse("2026-11-17T10:00:00Z");

    final Reports reports = new Reports();
    final Comments comments = new Comments();
    final List<Object> events = new ArrayList<>();
    final List<Long> moderated = new ArrayList<>();
    final ReportService service = new ReportService(reports, comments,
            id -> Optional.of(new BroadcastInfo(id, 7L, "방장", "두산 편파중계", "LIVE", 9L, "두산 베어스")),
            (id, reason) -> moderated.add(id), events::add, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void 댓글을_신고하면_내용을_남기고_같은_사람은_두_번_신고할_수_없다() {
        Comment c = comments.save(new Comment(null, 1L, 5L, "악플러", "나쁜 말", NOW, NOW));

        Report r = service.report(2L, "곰돌", ReportType.COMMENT, c.getId(), ReportReason.ABUSE, "");

        assertThat(r.snapshot()).isEqualTo("악플러: 나쁜 말");
        assertThat(r.subjectUserId()).isEqualTo(5L);
        assertThatThrownBy(() -> service.report(2L, "곰돌", ReportType.COMMENT, c.getId(), ReportReason.SPAM, ""))
                .hasMessageContaining("이미 신고");
    }

    @Test
    void 내_방송은_신고할_수_없다() {
        assertThatThrownBy(() -> service.report(7L, "방장", ReportType.BROADCAST, 1L, ReportReason.ETC, ""))
                .hasMessageContaining("신고할 수 없습니다");
    }

    @Test
    void 댓글_숨김으로_처리하면_댓글이_내려가고_같은_대상_신고가_함께_닫힌다() {
        Comment c = comments.save(new Comment(null, 1L, 5L, "악플러", "나쁜 말", NOW, NOW));
        Report first = service.report(2L, "곰돌", ReportType.COMMENT, c.getId(), ReportReason.ABUSE, "");
        service.report(3L, "잠실", ReportType.COMMENT, c.getId(), ReportReason.ABUSE, "");

        int closed = service.resolve(1L, first.id(), ModerationAction.HIDE_COMMENT, null);

        assertThat(closed).isEqualTo(2);
        assertThat(comments.byId).isEmpty();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(CommentDeleted.class);
    }

    @Test
    void 방송_신고에는_방송_중단만_쓸_수_있고_중단은_방송_서비스에_요청한다() {
        Report r = service.report(2L, "곰돌", ReportType.BROADCAST, 1L, ReportReason.RIGHTS_VIOLATION, "TV 화면 송출");

        assertThatThrownBy(() -> service.resolve(1L, r.id(), ModerationAction.HIDE_COMMENT, null))
                .hasMessageContaining("쓸 수 없는 조치");
        service.resolve(1L, r.id(), ModerationAction.END_BROADCAST, "중계권");

        assertThat(moderated).containsExactly(1L);
        assertThat(reports.rows.get(r.id()).status()).isEqualTo(ReportStatus.ACTIONED);
    }

    static class Comments implements CommentRepositoryPort {
        final Map<Long, Comment> byId = new HashMap<>();
        long seq;

        @Override
        public Comment save(Comment c) {
            Comment saved = new Comment(c.getId() == null ? ++seq : c.getId(), c.getBroadcastId(), c.getAuthorId(),
                    c.getAuthorNickname(), c.getContent(), c.getCreatedAt(), c.getUpdatedAt());
            byId.put(saved.getId(), saved);
            return saved;
        }

        @Override
        public Optional<Comment> findById(Long id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public PageResult<Comment> findLatestByBroadcast(Long broadcastId, int page, int size) {
            return new PageResult<>(List.copyOf(byId.values()), page, size, byId.size(), 1);
        }

        @Override
        public void delete(Long id) {
            byId.remove(id);
        }
    }

    static class Reports implements ReportRepositoryPort {
        final Map<Long, Report> rows = new HashMap<>();
        long seq;

        @Override
        public Optional<Report> insert(Report r) {
            boolean dup = rows.values().stream().anyMatch(x -> x.reporterId().equals(r.reporterId())
                    && x.type() == r.type() && x.refId().equals(r.refId()));
            if (dup) {
                return Optional.empty();
            }
            Report saved = new Report(++seq, r.type(), r.refId(), r.broadcastId(), r.reporterId(),
                    r.reporterNickname(), r.reason(), r.detail(), r.snapshot(), r.subjectUserId(), r.status(),
                    r.createdAt(), null, null);
            rows.put(saved.id(), saved);
            return Optional.of(saved);
        }

        @Override
        public Optional<Report> findById(Long id) {
            return Optional.ofNullable(rows.get(id));
        }

        @Override
        public PageResult<Report> findByStatus(ReportStatus status, int page, int size) {
            List<Report> list = rows.values().stream().filter(r -> r.status() == status).toList();
            return new PageResult<>(list, page, size, list.size(), 1);
        }

        @Override
        public long countOpen(ReportType type, Long refId) {
            return findOpenByTarget(type, refId).size();
        }

        @Override
        public int resolveAll(ReportType type, Long refId, ReportStatus status, Long adminId, Instant now) {
            int n = 0;
            for (Report r : findOpenByTarget(type, refId)) {
                rows.put(r.id(), new Report(r.id(), r.type(), r.refId(), r.broadcastId(), r.reporterId(),
                        r.reporterNickname(), r.reason(), r.detail(), r.snapshot(), r.subjectUserId(), status,
                        r.createdAt(), now, adminId));
                n++;
            }
            return n;
        }

        @Override
        public List<Report> findOpenByTarget(ReportType type, Long refId) {
            return rows.values().stream().filter(r -> r.type() == type && r.refId().equals(refId)
                    && r.status() == ReportStatus.OPEN).toList();
        }
    }
}
