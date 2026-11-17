package com.livebroadcast.fandom.out.persistence;

import static com.livebroadcast.fandom.out.persistence.JdbcSupport.instant;
import static com.livebroadcast.fandom.out.persistence.JdbcSupport.nullableLong;
import static com.livebroadcast.fandom.out.persistence.JdbcSupport.ts;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import com.livebroadcast.fandom.app.PageResult;
import com.livebroadcast.fandom.app.port.out.TargetRequestRepositoryPort;
import com.livebroadcast.fandom.domain.target.RequestStatus;
import com.livebroadcast.fandom.domain.target.TargetKind;
import com.livebroadcast.fandom.domain.target.TargetProfile;
import com.livebroadcast.fandom.domain.target.TargetRequest;

@Component
public class JdbcTargetRequestRepository implements TargetRequestRepositoryPort {

    private static final RowMapper<TargetRequest> MAPPER = (rs, i) -> new TargetRequest(
            rs.getLong("id"),
            rs.getLong("league_id"),
            TargetKind.valueOf(rs.getString("kind")),
            new TargetProfile(rs.getString("name"), rs.getString("name_en"), rs.getString("short_name"),
                    rs.getString("color_primary"), rs.getString("color_secondary"), rs.getString("description")),
            rs.getString("reference_url"),
            RequestStatus.valueOf(rs.getString("status")),
            rs.getString("reject_reason"),
            rs.getLong("requester_id"),
            rs.getString("requester_nickname"),
            rs.getInt("vote_count"),
            nullableLong(rs, "target_id"),
            nullableLong(rs, "decided_by"),
            instant(rs, "decided_at"),
            instant(rs, "created_at"));

    private final JdbcTemplate jdbc;

    public JdbcTargetRequestRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public TargetRequest save(TargetRequest r) {
        TargetProfile p = r.getProfile();
        if (r.getId() == null) {
            Long id = jdbc.queryForObject("""
                    INSERT INTO target_requests (league_id, kind, name, name_en, normalized_name, short_name,
                                                 color_primary, color_secondary, description, reference_url, status,
                                                 requester_id, requester_nickname, vote_count, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    RETURNING id
                    """, Long.class, r.getLeagueId(), r.getKind().name(), p.name(), p.nameEn(), p.normalizedName(),
                    p.shortName(), p.colorPrimary(), p.colorSecondary(), p.description(), r.getReferenceUrl(),
                    r.getStatus().name(), r.getRequesterId(), r.getRequesterNickname(), r.getVoteCount(),
                    ts(r.getCreatedAt()));
            return findById(id).orElseThrow();
        }
        jdbc.update("""
                UPDATE target_requests SET status = ?, reject_reason = ?, vote_count = ?, target_id = ?,
                                           decided_by = ?, decided_at = ?
                WHERE id = ?
                """, r.getStatus().name(), r.getRejectReason(), r.getVoteCount(), r.getTargetId(), r.getDecidedBy(),
                ts(r.getDecidedAt()), r.getId());
        return r;
    }

    @Override
    public Optional<TargetRequest> findById(Long id) {
        return jdbc.query("SELECT * FROM target_requests WHERE id = ?", MAPPER, id).stream().findFirst();
    }

    @Override
    public Optional<TargetRequest> findByIdForUpdate(Long id) {
        return jdbc.query("SELECT * FROM target_requests WHERE id = ? FOR UPDATE", MAPPER, id).stream().findFirst();
    }

    @Override
    public Optional<TargetRequest> findOpenByLeagueAndName(Long leagueId, String normalizedName) {
        return jdbc.query("""
                SELECT * FROM target_requests
                WHERE league_id = ? AND normalized_name = ? AND status = 'PENDING'
                """, MAPPER, leagueId, normalizedName).stream().findFirst();
    }

    @Override
    public PageResult<TargetRequest> findByStatus(RequestStatus status, Long leagueId, int page, int size) {
        List<Object> args = new ArrayList<>();
        args.add(status.name());
        String where = "status = ?";
        if (leagueId != null) {
            where += " AND league_id = ?";
            args.add(leagueId);
        }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM target_requests WHERE " + where, Long.class,
                args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((long) page * size);
        List<TargetRequest> content = jdbc.query("SELECT * FROM target_requests WHERE " + where
                + " ORDER BY vote_count DESC, created_at ASC LIMIT ? OFFSET ?", MAPPER, pageArgs.toArray());
        long totalElements = total == null ? 0 : total;
        return new PageResult<>(content, page, size, totalElements, (int) ((totalElements + size - 1) / size));
    }

    @Override
    public List<TargetRequest> findByRequester(Long requesterId) {
        return jdbc.query("""
                SELECT r.* FROM target_requests r
                WHERE r.requester_id = ?
                   OR r.id IN (SELECT request_id FROM target_request_votes WHERE user_id = ?)
                ORDER BY r.created_at DESC
                LIMIT 50
                """, MAPPER, requesterId, requesterId);
    }

    @Override
    public boolean insertVote(Long requestId, Long userId, String nickname, Instant now) {
        int inserted = jdbc.update("""
                INSERT INTO target_request_votes (request_id, user_id, user_nickname, created_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (request_id, user_id) DO NOTHING
                """, requestId, userId, nickname == null ? "" : nickname, ts(now));
        return inserted == 1;
    }

    @Override
    public boolean hasVoted(Long requestId, Long userId) {
        Boolean voted = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM target_request_votes WHERE request_id = ? AND user_id = ?)",
                Boolean.class, requestId, userId);
        return Boolean.TRUE.equals(voted);
    }

    @Override
    public List<Voter> findVoters(Long requestId) {
        return jdbc.query("SELECT user_id, user_nickname FROM target_request_votes WHERE request_id = ? ORDER BY created_at",
                (rs, i) -> new Voter(rs.getLong("user_id"), rs.getString("user_nickname")), requestId);
    }
}
