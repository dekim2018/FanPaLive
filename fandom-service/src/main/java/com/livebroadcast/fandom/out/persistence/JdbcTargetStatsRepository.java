package com.livebroadcast.fandom.out.persistence;

import static com.livebroadcast.fandom.out.persistence.JdbcSupport.date;
import static com.livebroadcast.fandom.out.persistence.JdbcSupport.instant;
import static com.livebroadcast.fandom.out.persistence.JdbcSupport.ts;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.livebroadcast.fandom.app.port.out.TargetStatsPort;
import com.livebroadcast.fandom.domain.fan.FanChange;
import com.livebroadcast.fandom.domain.ranking.HeartDailyDelta;
import com.livebroadcast.fandom.domain.ranking.TargetActivity;
import com.livebroadcast.fandom.domain.ranking.TargetStats;

@Component
public class JdbcTargetStatsRepository implements TargetStatsPort {

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;

    public JdbcTargetStatsRepository(JdbcTemplate jdbc, NamedParameterJdbcTemplate named) {
        this.jdbc = jdbc;
        this.named = named;
    }

    @Override
    public void applyFanChanges(List<FanChange> changes, Instant at) {
        List<Object[]> args = new ArrayList<>();
        for (FanChange c : changes) {
            args.add(new Object[]{c.targetId(), (long) c.primaryDelta(), (long) c.followDelta(), ts(at)});
        }
        jdbc.batchUpdate("""
                INSERT INTO target_stats (target_id, primary_fans, follow_fans, updated_at)
                VALUES (?, GREATEST(?, 0), GREATEST(?, 0), ?)
                ON CONFLICT (target_id) DO UPDATE
                SET primary_fans = GREATEST(target_stats.primary_fans + ?, 0),
                    follow_fans  = GREATEST(target_stats.follow_fans + ?, 0),
                    updated_at   = EXCLUDED.updated_at
                """, args.stream().map(a -> new Object[]{a[0], a[1], a[2], a[3], a[1], a[2]}).toList());
    }

    @Override
    public void addHearts(List<HeartDailyDelta> deltas) {
        // 같은 행을 갱신하는 문장이 대상 수만큼만 실행된다(하트 수가 아님).
        jdbc.batchUpdate("""
                INSERT INTO target_stats_daily (target_id, stat_date, hearts, heart_points, suspicious_hearts,
                                                last_activity_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (target_id, stat_date) DO UPDATE
                SET hearts            = target_stats_daily.hearts + EXCLUDED.hearts,
                    heart_points      = target_stats_daily.heart_points + EXCLUDED.heart_points,
                    suspicious_hearts = target_stats_daily.suspicious_hearts + EXCLUDED.suspicious_hearts,
                    last_activity_at  = GREATEST(COALESCE(target_stats_daily.last_activity_at, EXCLUDED.last_activity_at),
                                                 EXCLUDED.last_activity_at)
                """, deltas.stream().map(d -> new Object[]{d.targetId(), date(d.date()), d.hearts(), d.heartPoints(),
                d.suspiciousHearts(), ts(d.lastAt())}).toList());
        jdbc.batchUpdate("""
                UPDATE target_stats SET hearts_total = hearts_total + ?, updated_at = NOW() WHERE target_id = ?
                """, deltas.stream().map(d -> new Object[]{d.hearts(), d.targetId()}).toList());
    }

    @Override
    public void addDonation(Long targetId, LocalDate date, long amount, Instant at) {
        jdbc.update("""
                INSERT INTO target_stats_daily (target_id, stat_date, donation_amount, last_activity_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (target_id, stat_date) DO UPDATE
                SET donation_amount  = target_stats_daily.donation_amount + EXCLUDED.donation_amount,
                    last_activity_at = GREATEST(COALESCE(target_stats_daily.last_activity_at, EXCLUDED.last_activity_at),
                                                EXCLUDED.last_activity_at)
                """, targetId, date(date), amount, ts(at));
        jdbc.update("UPDATE target_stats SET donation_total = donation_total + ?, updated_at = NOW() WHERE target_id = ?",
                amount, targetId);
    }

    @Override
    public long overwriteDailyDonation(Long targetId, LocalDate date, long amount) {
        Long before = jdbc.query("""
                SELECT donation_amount FROM target_stats_daily WHERE target_id = ? AND stat_date = ? FOR UPDATE
                """, (rs, i) -> rs.getLong(1), targetId, date(date)).stream().findFirst().orElse(0L);
        jdbc.update("""
                INSERT INTO target_stats_daily (target_id, stat_date, donation_amount) VALUES (?, ?, ?)
                ON CONFLICT (target_id, stat_date) DO UPDATE SET donation_amount = EXCLUDED.donation_amount
                """, targetId, date(date), amount);
        long diff = amount - before;
        if (diff != 0) {
            jdbc.update("""
                    UPDATE target_stats SET donation_total = GREATEST(donation_total + ?, 0), updated_at = NOW()
                    WHERE target_id = ?
                    """, diff, targetId);
        }
        return diff;
    }

    @Override
    public Map<Long, TargetStats> findStats(Collection<Long> targetIds) {
        Map<Long, TargetStats> result = new HashMap<>();
        if (targetIds.isEmpty()) {
            return result;
        }
        named.query("SELECT * FROM target_stats WHERE target_id IN (:ids)", Map.of("ids", targetIds), rs -> {
            long id = rs.getLong("target_id");
            result.put(id, new TargetStats(id, rs.getLong("primary_fans"), rs.getLong("follow_fans"),
                    rs.getLong("hearts_total"), rs.getLong("donation_total")));
        });
        return result;
    }

    @Override
    public Map<Long, TargetActivity> findActivities(Collection<Long> targetIds, LocalDate from, LocalDate toExclusive) {
        Map<Long, TargetActivity> result = new HashMap<>();
        if (targetIds.isEmpty()) {
            return result;
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ids", targetIds)
                .addValue("from", date(from))
                .addValue("to", date(toExclusive));
        named.query("""
                SELECT target_id, SUM(hearts)::bigint AS hearts, SUM(heart_points)::bigint AS heart_points,
                       SUM(donation_amount)::bigint AS donation_amount, MAX(last_activity_at) AS last_activity_at
                FROM target_stats_daily
                WHERE target_id IN (:ids) AND stat_date >= :from AND stat_date < :to
                GROUP BY target_id
                """, params, rs -> {
            long id = rs.getLong("target_id");
            result.put(id, new TargetActivity(id, rs.getLong("hearts"), rs.getLong("heart_points"),
                    rs.getLong("donation_amount"), instant(rs, "last_activity_at")));
        });
        return result;
    }

    @Override
    public Map<Long, Long> findDailyDonations(LocalDate date) {
        Map<Long, Long> result = new HashMap<>();
        jdbc.query("SELECT target_id, donation_amount FROM target_stats_daily WHERE stat_date = ? AND donation_amount <> 0",
                rs -> {
                    result.put(rs.getLong("target_id"), rs.getLong("donation_amount"));
                }, date(date));
        return result;
    }

    @Override
    public List<Long> findTopByFans(int limit) {
        return jdbc.query("""
                SELECT s.target_id FROM target_stats s JOIN fan_targets t ON t.id = s.target_id
                WHERE t.status = 'ACTIVE'
                ORDER BY (s.primary_fans + s.follow_fans) DESC, s.primary_fans DESC, s.target_id
                LIMIT ?
                """, (rs, i) -> rs.getLong(1), limit);
    }
}
