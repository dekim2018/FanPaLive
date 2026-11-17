package com.livebroadcast.fandom.out.persistence;

import static com.livebroadcast.fandom.out.persistence.JdbcSupport.date;
import static com.livebroadcast.fandom.out.persistence.JdbcSupport.ts;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.livebroadcast.fandom.app.port.out.PointLedgerPort;
import com.livebroadcast.fandom.domain.level.FanProfile;
import com.livebroadcast.fandom.domain.level.PointReason;

/**
 * 원장에 새로 들어간 행만 합계(fan_profiles)에 더한다. INSERT ... RETURNING 을 CTE 로 이어서
 * "원장에 넣기"와 "합계 더하기"를 한 문장으로 처리하므로, 중복 이벤트가 와도 합계가 두 번 늘지 않는다.
 */
@Component
public class JdbcPointLedgerRepository implements PointLedgerPort {

    private final JdbcTemplate jdbc;

    public JdbcPointLedgerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean award(Long userId, String nickname, Long targetId, PointReason reason, String refKey, int points,
                         Instant now) {
        String safeNick = nickname == null || nickname.isBlank() ? "팬" : nickname;
        Integer applied = jdbc.queryForObject("""
                WITH ins AS (
                    INSERT INTO fan_point_ledger (user_id, user_nickname, target_id, reason, ref_key, points, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (user_id, reason, ref_key) DO NOTHING
                    RETURNING user_id, user_nickname, points, reason
                ), up AS (
                    INSERT INTO fan_profiles (user_id, nickname, points, heart_days, donation_count, updated_at)
                    SELECT user_id, user_nickname, points,
                           CASE WHEN reason = 'HEART' THEN 1 ELSE 0 END,
                           CASE WHEN reason = 'DONATION' THEN 1 ELSE 0 END, ?
                    FROM ins
                    ON CONFLICT (user_id) DO UPDATE SET
                        points = fan_profiles.points + EXCLUDED.points,
                        heart_days = fan_profiles.heart_days + EXCLUDED.heart_days,
                        donation_count = fan_profiles.donation_count + EXCLUDED.donation_count,
                        nickname = EXCLUDED.nickname,
                        updated_at = EXCLUDED.updated_at
                    RETURNING 1
                )
                SELECT COUNT(*)::int FROM up
                """, Integer.class, userId, safeNick, targetId, reason.name(), refKey, points, ts(now), ts(now));
        return applied != null && applied > 0;
    }

    @Override
    public int awardDailyHearts(LocalDate date, int pointsPerHeart, Instant now) {
        Integer n = jdbc.queryForObject("""
                WITH ins AS (
                    INSERT INTO fan_point_ledger (user_id, user_nickname, target_id, reason, ref_key, points, created_at)
                    SELECT user_id, MAX(user_nickname), NULL, 'HEART', ?, (SUM(amount) * ?)::int, ?
                    FROM heart_logs
                    WHERE given_date = ? AND NOT suspicious
                    GROUP BY user_id
                    ON CONFLICT (user_id, reason, ref_key) DO NOTHING
                    RETURNING user_id, user_nickname, points
                ), up AS (
                    INSERT INTO fan_profiles (user_id, nickname, points, heart_days, donation_count, updated_at)
                    SELECT user_id, user_nickname, points, 1, 0, ? FROM ins
                    ON CONFLICT (user_id) DO UPDATE SET
                        points = fan_profiles.points + EXCLUDED.points,
                        heart_days = fan_profiles.heart_days + 1,
                        nickname = EXCLUDED.nickname,
                        updated_at = EXCLUDED.updated_at
                    RETURNING 1
                )
                SELECT COUNT(*)::int FROM up
                """, Integer.class, date.toString(), pointsPerHeart, ts(now), date(date), ts(now));
        return n == null ? 0 : n;
    }

    @Override
    public FanProfile findProfile(Long userId) {
        return findProfiles(List.of(userId)).getOrDefault(userId, FanProfile.empty(userId));
    }

    @Override
    public Map<Long, FanProfile> findProfiles(Collection<Long> userIds) {
        Map<Long, FanProfile> result = new HashMap<>();
        if (userIds.isEmpty()) {
            return result;
        }
        String marks = String.join(",", userIds.stream().map(x -> "?").toList());
        List<Object> args = new ArrayList<>(userIds);
        args.addAll(userIds);
        jdbc.query("""
                SELECT u.user_id, p.nickname, COALESCE(p.points, 0) AS points,
                       COALESCE(p.heart_days, 0) AS heart_days, COALESCE(p.donation_count, 0) AS donation_count,
                       COALESCE(s.hits, 0) AS hits, COALESCE(s.total, 0) AS total
                FROM (SELECT DISTINCT UNNEST(ARRAY[%s]::bigint[]) AS user_id) u
                LEFT JOIN fan_profiles p ON p.user_id = u.user_id
                LEFT JOIN (
                    SELECT user_id, COUNT(*) FILTER (WHERE result = 'HIT') AS hits,
                           COUNT(*) FILTER (WHERE result IN ('HIT', 'MISS')) AS total
                    FROM match_predictions WHERE user_id IN (%s) GROUP BY user_id
                ) s ON s.user_id = u.user_id
                """.formatted(marks, marks), rs -> {
            long id = rs.getLong("user_id");
            result.put(id, new FanProfile(id, rs.getString("nickname"), rs.getLong("points"), rs.getInt("hits"),
                    rs.getInt("total"), rs.getInt("heart_days"), rs.getInt("donation_count")));
        }, args.toArray());
        return result;
    }
}
