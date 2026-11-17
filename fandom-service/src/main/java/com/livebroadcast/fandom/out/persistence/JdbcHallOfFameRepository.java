package com.livebroadcast.fandom.out.persistence;

import static com.livebroadcast.fandom.out.persistence.JdbcSupport.ts;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.livebroadcast.fandom.app.port.out.HallOfFamePort;
import com.livebroadcast.fandom.domain.ranking.SeasonRecord;
import com.livebroadcast.fandom.domain.ranking.SeasonRecord.SeasonRow;
import com.livebroadcast.fandom.domain.ranking.Supporter;

@Component
public class JdbcHallOfFameRepository implements HallOfFamePort {

    private final JdbcTemplate jdbc;

    public JdbcHallOfFameRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void saveMvp(Long leagueId, String seasonKey, Long targetId, Supporter mvp, Instant now) {
        jdbc.update("""
                INSERT INTO season_mvps (league_id, season_key, target_id, user_id, nickname, points, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (league_id, season_key) DO UPDATE SET target_id = EXCLUDED.target_id,
                    user_id = EXCLUDED.user_id, nickname = EXCLUDED.nickname, points = EXCLUDED.points,
                    created_at = EXCLUDED.created_at
                """, leagueId, seasonKey, targetId, mvp.userId(), mvp.nickname(), mvp.points(), ts(now));
    }

    @Override
    public List<SeasonRecord> findSeasons(Long leagueId, int topN, int seasons) {
        Map<String, List<SeasonRow>> rows = new LinkedHashMap<>();
        jdbc.query("""
                SELECT s.period_key, s.rank, s.score, t.id, t.name, t.short_name, t.color_primary, t.color_secondary
                FROM ranking_snapshots s
                JOIN fan_targets t ON t.id = s.target_id
                WHERE s.league_id = ? AND s.period_type = 'SEASON' AND s.rank <= ?
                  AND s.period_key IN (
                      SELECT DISTINCT period_key FROM ranking_snapshots
                      WHERE league_id = ? AND period_type = 'SEASON'
                      ORDER BY period_key DESC LIMIT ?)
                ORDER BY s.period_key DESC, s.rank
                """, rs -> {
            rows.computeIfAbsent(rs.getString("period_key"), k -> new ArrayList<>()).add(new SeasonRow(
                    rs.getInt("rank"), rs.getLong("id"), rs.getString("name"), rs.getString("short_name"),
                    rs.getString("color_primary"), rs.getString("color_secondary"), rs.getLong("score")));
        }, leagueId, topN, leagueId, seasons);
        Map<String, Supporter> mvps = new LinkedHashMap<>();
        jdbc.query("SELECT season_key, user_id, nickname, points FROM season_mvps WHERE league_id = ?", rs -> {
            mvps.put(rs.getString("season_key"), new Supporter(rs.getLong("user_id"), rs.getString("nickname"),
                    rs.getLong("points"), 0, 0));
        }, leagueId);
        List<SeasonRecord> result = new ArrayList<>();
        rows.forEach((key, list) -> result.add(new SeasonRecord(key, list, mvps.get(key))));
        return result;
    }
}
