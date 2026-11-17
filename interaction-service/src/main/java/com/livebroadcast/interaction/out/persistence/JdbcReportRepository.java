package com.livebroadcast.interaction.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import com.livebroadcast.interaction.app.PageResult;
import com.livebroadcast.interaction.app.port.out.ReportRepositoryPort;
import com.livebroadcast.interaction.domain.report.Report;
import com.livebroadcast.interaction.domain.report.ReportReason;
import com.livebroadcast.interaction.domain.report.ReportStatus;
import com.livebroadcast.interaction.domain.report.ReportType;

@Component
public class JdbcReportRepository implements ReportRepositoryPort {

    private static final RowMapper<Report> MAPPER = JdbcReportRepository::map;

    private final JdbcTemplate jdbc;

    public JdbcReportRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Report> insert(Report r) {
        return jdbc.query("""
                INSERT INTO reports (type, ref_id, broadcast_id, reporter_id, reporter_nickname, reason, detail,
                                     snapshot, subject_user_id, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'OPEN', ?)
                ON CONFLICT (reporter_id, type, ref_id) DO NOTHING
                RETURNING *
                """, MAPPER, r.type().name(), r.refId(), r.broadcastId(), r.reporterId(), r.reporterNickname(),
                r.reason().name(), r.detail(), r.snapshot(), r.subjectUserId(), Timestamp.from(r.createdAt()))
                .stream().findFirst();
    }

    @Override
    public Optional<Report> findById(Long id) {
        return jdbc.query("SELECT * FROM reports WHERE id = ?", MAPPER, id).stream().findFirst();
    }

    @Override
    public PageResult<Report> findByStatus(ReportStatus status, int page, int size) {
        List<Report> content = jdbc.query("SELECT * FROM reports WHERE status = ? ORDER BY id DESC LIMIT ? OFFSET ?",
                MAPPER, status.name(), size, (long) page * size);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM reports WHERE status = ?", Long.class, status.name());
        long t = total == null ? 0 : total;
        return new PageResult<>(content, page, size, t, (int) Math.ceil(t / (double) size));
    }

    @Override
    public long countOpen(ReportType type, Long refId) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM reports WHERE type = ? AND ref_id = ? AND status = 'OPEN'",
                Long.class, type.name(), refId);
        return n == null ? 0 : n;
    }

    @Override
    public int resolveAll(ReportType type, Long refId, ReportStatus status, Long adminId, Instant now) {
        return jdbc.update("""
                UPDATE reports SET status = ?, resolved_at = ?, resolved_by = ?
                WHERE type = ? AND ref_id = ? AND status = 'OPEN'
                """, status.name(), Timestamp.from(now), adminId, type.name(), refId);
    }

    @Override
    public List<Report> findOpenByTarget(ReportType type, Long refId) {
        return jdbc.query("SELECT * FROM reports WHERE type = ? AND ref_id = ? AND status = 'OPEN' ORDER BY id",
                MAPPER, type.name(), refId);
    }

    private static Report map(ResultSet rs, int i) throws SQLException {
        Timestamp resolved = rs.getTimestamp("resolved_at");
        long broadcastId = rs.getLong("broadcast_id");
        Long bid = rs.wasNull() ? null : broadcastId;
        long subject = rs.getLong("subject_user_id");
        Long sid = rs.wasNull() ? null : subject;
        long by = rs.getLong("resolved_by");
        Long resolvedBy = rs.wasNull() ? null : by;
        return new Report(rs.getLong("id"), ReportType.valueOf(rs.getString("type")), rs.getLong("ref_id"), bid,
                rs.getLong("reporter_id"), rs.getString("reporter_nickname"),
                ReportReason.valueOf(rs.getString("reason")), rs.getString("detail"), rs.getString("snapshot"), sid,
                ReportStatus.valueOf(rs.getString("status")), rs.getTimestamp("created_at").toInstant(),
                resolved == null ? null : resolved.toInstant(), resolvedBy);
    }
}
