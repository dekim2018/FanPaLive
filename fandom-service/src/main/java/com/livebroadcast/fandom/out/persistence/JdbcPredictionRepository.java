package com.livebroadcast.fandom.out.persistence;

import static com.livebroadcast.fandom.out.persistence.JdbcSupport.instant;
import static com.livebroadcast.fandom.out.persistence.JdbcSupport.ts;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import com.livebroadcast.fandom.app.port.out.PredictionRepositoryPort;
import com.livebroadcast.fandom.domain.match.MatchSide;
import com.livebroadcast.fandom.domain.prediction.Prediction;
import com.livebroadcast.fandom.domain.prediction.PredictionResult;
import com.livebroadcast.fandom.domain.prediction.PredictionRules.Outcome;
import com.livebroadcast.fandom.domain.prediction.PredictionTally;

@Component
public class JdbcPredictionRepository implements PredictionRepositoryPort {

    private static final RowMapper<Prediction> MAPPER = (rs, i) -> new Prediction(rs.getLong("match_id"),
            rs.getLong("user_id"), rs.getString("user_nickname"), MatchSide.valueOf(rs.getString("pick")),
            instant(rs, "created_at"),
            rs.getString("result") == null ? null : PredictionResult.valueOf(rs.getString("result")),
            instant(rs, "settled_at"));

    private final JdbcTemplate jdbc;

    public JdbcPredictionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void upsert(Long matchId, Long userId, String nickname, MatchSide pick, Instant now) {
        jdbc.update("""
                INSERT INTO match_predictions (match_id, user_id, user_nickname, pick, created_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (match_id, user_id) DO UPDATE SET pick = EXCLUDED.pick, created_at = EXCLUDED.created_at
                WHERE match_predictions.result IS NULL
                """, matchId, userId, nickname, pick.name(), ts(now));
    }

    @Override
    public Optional<Prediction> find(Long matchId, Long userId) {
        return jdbc.query("SELECT * FROM match_predictions WHERE match_id = ? AND user_id = ?", MAPPER, matchId,
                userId).stream().findFirst();
    }

    @Override
    public PredictionTally tally(Long matchId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FILTER (WHERE pick = 'HOME') AS home, COUNT(*) FILTER (WHERE pick = 'AWAY') AS away
                FROM match_predictions WHERE match_id = ?
                """, (rs, i) -> new PredictionTally(rs.getLong("home"), rs.getLong("away")), matchId);
    }

    @Override
    public List<Prediction> settle(Long matchId, Outcome outcome, Instant now) {
        // 아직 정산하지 않은 예측만 바꾸고 바뀐 행을 돌려준다. 두 번 호출돼도 두 번째는 빈 목록이다.
        String winner = outcome.winner() == null ? null : outcome.winner().name();
        return jdbc.query("""
                UPDATE match_predictions
                SET result = CASE WHEN ?::varchar IS NULL THEN 'VOID' WHEN pick = ? THEN 'HIT' ELSE 'MISS' END,
                    settled_at = ?
                WHERE match_id = ? AND result IS NULL
                RETURNING *
                """, MAPPER, winner, winner, ts(now), matchId);
    }

    @Override
    public List<Prediction> findByUser(Long userId, int limit) {
        return jdbc.query("SELECT * FROM match_predictions WHERE user_id = ? ORDER BY created_at DESC LIMIT ?",
                MAPPER, userId, limit);
    }

    @Override
    public int[] hitsAndTotal(Long userId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FILTER (WHERE result = 'HIT') AS hits,
                       COUNT(*) FILTER (WHERE result IN ('HIT', 'MISS')) AS total
                FROM match_predictions WHERE user_id = ?
                """, (rs, i) -> new int[]{rs.getInt("hits"), rs.getInt("total")}, userId);
    }
}
