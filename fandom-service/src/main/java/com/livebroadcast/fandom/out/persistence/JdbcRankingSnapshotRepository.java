package com.livebroadcast.fandom.out.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.livebroadcast.fandom.app.port.out.RankingSnapshotPort;
import com.livebroadcast.fandom.domain.ranking.RankSnapshot;
import com.livebroadcast.fandom.domain.ranking.RankingPeriod;
import com.livebroadcast.fandom.domain.ranking.RankingRow;

@Component
public class JdbcRankingSnapshotRepository implements RankingSnapshotPort {

    private final JdbcTemplate jdbc;

    public JdbcRankingSnapshotRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(Long leagueId, RankingPeriod period, String periodKey, List<RankingRow> rows) {
        // 스케줄러가 두 번 돌아도 결과가 같도록 덮어쓴다.
        jdbc.batchUpdate("""
                INSERT INTO ranking_snapshots (league_id, period_type, period_key, target_id, rank, score, created_at)
                VALUES (?, ?, ?, ?, ?, ?, NOW())
                ON CONFLICT (league_id, period_type, period_key, target_id)
                DO UPDATE SET rank = EXCLUDED.rank, score = EXCLUDED.score, created_at = EXCLUDED.created_at
                """, rows.stream().map(r -> new Object[]{leagueId, period.name(), periodKey, r.targetId(), r.rank(),
                r.score()}).toList());
    }

    @Override
    public Map<Long, Integer> findRanks(Long leagueId, RankingPeriod period, String periodKey) {
        Map<Long, Integer> result = new HashMap<>();
        jdbc.query("""
                SELECT target_id, rank FROM ranking_snapshots
                WHERE league_id = ? AND period_type = ? AND period_key = ?
                """, rs -> {
            result.put(rs.getLong("target_id"), rs.getInt("rank"));
        }, leagueId, period.name(), periodKey);
        return result;
    }

    @Override
    public List<RankSnapshot> findHistory(Long targetId, RankingPeriod period, int limit) {
        List<RankSnapshot> recent = jdbc.query("""
                SELECT period_key, rank, score FROM ranking_snapshots
                WHERE target_id = ? AND period_type = ?
                ORDER BY period_key DESC
                LIMIT ?
                """, (rs, i) -> new RankSnapshot(rs.getString("period_key"), rs.getInt("rank"), rs.getLong("score")),
                targetId, period.name(), limit);
        return recent.reversed();
    }
}
