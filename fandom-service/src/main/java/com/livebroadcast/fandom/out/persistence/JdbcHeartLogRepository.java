package com.livebroadcast.fandom.out.persistence;

import static com.livebroadcast.fandom.out.persistence.JdbcSupport.date;
import static com.livebroadcast.fandom.out.persistence.JdbcSupport.ts;

import java.time.Instant;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.livebroadcast.fandom.app.port.out.HeartLogPort;
import com.livebroadcast.fandom.domain.heart.HeartGrant;
import com.livebroadcast.fandom.domain.ranking.ScoreFormula;
import com.livebroadcast.fandom.domain.ranking.Supporter;

@Component
public class JdbcHeartLogRepository implements HeartLogPort {

    private final JdbcTemplate jdbc;

    public JdbcHeartLogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insertBatch(List<HeartGrant> grants) {
        if (grants.isEmpty()) {
            return;
        }
        // PostgreSQL JDBC 드라이버는 reWriteBatchedInserts=true 면 여러 행 INSERT 한 문장으로 묶어 보낸다(application.yml).
        jdbc.batchUpdate("""
                INSERT INTO heart_logs (user_id, user_nickname, target_id, match_id, amount, points, source,
                                        suspicious, given_date, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, grants.stream().map(g -> new Object[]{g.userId(),
                g.userNickname() == null ? "" : g.userNickname(), g.targetId(), g.matchId(), g.amount(), g.points(),
                g.source().name(), g.suspicious(), date(g.givenDate()), ts(g.createdAt())}).toList());
    }

    /**
     * 하트 점수와 후원 점수(100원당 1점)를 사용자별로 합친다. 의심 하트는 빼고 계산한다.
     */
    @Override
    public List<Supporter> findTopSupporters(Long targetId, Instant from, Instant to, int limit) {
        return jdbc.query("""
                SELECT user_id, MAX(nickname) AS nickname, SUM(points)::bigint AS points, SUM(hearts)::bigint AS hearts,
                       SUM(donation)::bigint AS donation
                FROM (
                    SELECT user_id, user_nickname AS nickname, SUM(amount * points)::bigint AS points,
                           SUM(amount)::bigint AS hearts, 0::bigint AS donation
                    FROM heart_logs
                    WHERE target_id = ? AND created_at >= ? AND created_at < ? AND NOT suspicious
                    GROUP BY user_id, user_nickname
                    UNION ALL
                    SELECT donor_id, donor_nickname, FLOOR(SUM(amount) / ?)::bigint, 0, SUM(amount)::bigint
                    FROM donation_contributions
                    WHERE target_id = ? AND approved_at >= ? AND approved_at < ?
                    GROUP BY donor_id, donor_nickname
                ) contributions
                GROUP BY user_id
                ORDER BY points DESC, user_id
                LIMIT ?
                """, (rs, i) -> new Supporter(rs.getLong("user_id"), rs.getString("nickname"), rs.getLong("points"),
                        rs.getLong("hearts"), rs.getLong("donation")),
                targetId, ts(from), ts(to), ScoreFormula.WON_PER_POINT, targetId, ts(from), ts(to), limit);
    }
}
