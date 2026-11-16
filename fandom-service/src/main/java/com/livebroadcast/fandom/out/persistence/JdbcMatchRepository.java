package com.livebroadcast.fandom.out.persistence;

import static com.livebroadcast.fandom.out.persistence.JdbcSupport.instant;
import static com.livebroadcast.fandom.out.persistence.JdbcSupport.nullableLong;
import static com.livebroadcast.fandom.out.persistence.JdbcSupport.ts;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import com.livebroadcast.fandom.app.port.out.MatchRepositoryPort;
import com.livebroadcast.fandom.domain.match.Match;
import com.livebroadcast.fandom.domain.match.MatchStatus;

@Component
public class JdbcMatchRepository implements MatchRepositoryPort {

    private static final RowMapper<Match> MAPPER = (rs, i) -> new Match(rs.getLong("id"), rs.getLong("league_id"),
            rs.getLong("home_target_id"), rs.getLong("away_target_id"), instant(rs, "starts_at"),
            MatchStatus.valueOf(rs.getString("status")), rs.getInt("home_score"), rs.getInt("away_score"),
            rs.getString("venue"), nullableLong(rs, "created_by"), instant(rs, "created_at"),
            instant(rs, "updated_at"));

    private final JdbcTemplate jdbc;

    public JdbcMatchRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Match save(Match m) {
        if (m.getId() == null) {
            Long id = jdbc.queryForObject("""
                    INSERT INTO matches (league_id, home_target_id, away_target_id, starts_at, status, home_score,
                                         away_score, venue, created_by, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    RETURNING id
                    """, Long.class, m.getLeagueId(), m.getHomeTargetId(), m.getAwayTargetId(), ts(m.getStartsAt()),
                    m.getStatus().name(), m.getHomeScore(), m.getAwayScore(), m.getVenue(), m.getCreatedBy(),
                    ts(m.getCreatedAt()), ts(m.getUpdatedAt()));
            return findById(id).orElseThrow();
        }
        jdbc.update("""
                UPDATE matches SET starts_at = ?, status = ?, home_score = ?, away_score = ?, venue = ?, updated_at = ?
                WHERE id = ?
                """, ts(m.getStartsAt()), m.getStatus().name(), m.getHomeScore(), m.getAwayScore(), m.getVenue(),
                ts(m.getUpdatedAt()), m.getId());
        return m;
    }

    @Override
    public Optional<Match> findById(Long id) {
        return jdbc.query("SELECT * FROM matches WHERE id = ?", MAPPER, id).stream().findFirst();
    }

    @Override
    public List<Match> findByStartsAtBetween(Instant from, Instant to, Long leagueId) {
        if (leagueId == null) {
            return jdbc.query("SELECT * FROM matches WHERE starts_at >= ? AND starts_at < ? ORDER BY starts_at, id",
                    MAPPER, ts(from), ts(to));
        }
        return jdbc.query("""
                SELECT * FROM matches WHERE starts_at >= ? AND starts_at < ? AND league_id = ? ORDER BY starts_at, id
                """, MAPPER, ts(from), ts(to), leagueId);
    }

    @Override
    public List<Match> findLiveOrUpcoming(Instant from, int limit) {
        return jdbc.query("""
                SELECT * FROM matches
                WHERE status = 'LIVE' OR (status = 'SCHEDULED' AND starts_at >= ?)
                ORDER BY CASE status WHEN 'LIVE' THEN 0 ELSE 1 END, starts_at
                LIMIT ?
                """, MAPPER, ts(from), limit);
    }
}
