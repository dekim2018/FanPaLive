package com.livebroadcast.fandom.out.persistence;

import static com.livebroadcast.fandom.out.persistence.JdbcSupport.instant;
import static com.livebroadcast.fandom.out.persistence.JdbcSupport.ts;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.livebroadcast.fandom.app.port.out.FanRepositoryPort;
import com.livebroadcast.fandom.domain.fan.FanMembership;

@Component
public class JdbcFanRepository implements FanRepositoryPort {

    /** pg_advisory_xact_lock 키 공간을 다른 용도와 나누기 위한 접두 값 */
    private static final long USER_LOCK_NAMESPACE = 7_000_000_000L;

    private static final RowMapper<FanMembership> MAPPER = (rs, i) -> new FanMembership(rs.getLong("user_id"),
            rs.getLong("target_id"), rs.getLong("league_id"), rs.getString("user_nickname"),
            rs.getBoolean("primary_fan"), instant(rs, "joined_at"));

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;

    public JdbcFanRepository(JdbcTemplate jdbc, NamedParameterJdbcTemplate named) {
        this.jdbc = jdbc;
        this.named = named;
    }

    @Override
    public Optional<FanMembership> find(Long userId, Long targetId) {
        return jdbc.query("SELECT * FROM fan_memberships WHERE user_id = ? AND target_id = ?", MAPPER, userId,
                targetId).stream().findFirst();
    }

    @Override
    public Optional<FanMembership> findPrimary(Long userId, Long leagueId) {
        return jdbc.query("SELECT * FROM fan_memberships WHERE user_id = ? AND league_id = ? AND primary_fan",
                MAPPER, userId, leagueId).stream().findFirst();
    }

    @Override
    public FanMembership save(FanMembership m) {
        jdbc.update("""
                INSERT INTO fan_memberships (user_id, target_id, league_id, user_nickname, primary_fan, joined_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id, target_id) DO UPDATE SET primary_fan = EXCLUDED.primary_fan,
                                                              user_nickname = EXCLUDED.user_nickname
                """, m.userId(), m.targetId(), m.leagueId(), m.userNickname() == null ? "" : m.userNickname(),
                m.primary(), ts(m.joinedAt()));
        return m;
    }

    @Override
    public void delete(Long userId, Long targetId) {
        jdbc.update("DELETE FROM fan_memberships WHERE user_id = ? AND target_id = ?", userId, targetId);
    }

    @Override
    public List<FanMembership> findByUser(Long userId) {
        return jdbc.query("SELECT * FROM fan_memberships WHERE user_id = ? ORDER BY primary_fan DESC, joined_at",
                MAPPER, userId);
    }

    @Override
    public List<FanMembership> findPrimaryByUsers(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return named.query("""
                SELECT * FROM fan_memberships WHERE primary_fan AND user_id IN (:ids)
                ORDER BY user_id, league_id
                """, Map.of("ids", userIds), MAPPER);
    }

    @Override
    public Optional<Instant> findLastPrimaryChange(Long userId, Long leagueId) {
        return jdbc.query("SELECT changed_at FROM primary_fan_changes WHERE user_id = ? AND league_id = ?",
                (rs, i) -> instant(rs, "changed_at"), userId, leagueId).stream().findFirst();
    }

    @Override
    public void recordPrimaryChange(Long userId, Long leagueId, Instant at) {
        jdbc.update("""
                INSERT INTO primary_fan_changes (user_id, league_id, changed_at) VALUES (?, ?, ?)
                ON CONFLICT (user_id, league_id) DO UPDATE SET changed_at = EXCLUDED.changed_at
                """, userId, leagueId, ts(at));
    }

    @Override
    public void lockUser(Long userId) {
        // 트랜잭션이 끝나면 자동으로 풀리는 잠금. 같은 사용자의 팬 등록 변경만 순서대로 처리된다.
        jdbc.query("SELECT pg_advisory_xact_lock(?)", (ResultSetExtractor<Object>) rs -> null,
                USER_LOCK_NAMESPACE + userId);
    }
}
