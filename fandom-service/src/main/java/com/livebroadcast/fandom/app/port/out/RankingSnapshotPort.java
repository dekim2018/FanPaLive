package com.livebroadcast.fandom.app.port.out;

import java.util.List;
import java.util.Map;

import com.livebroadcast.fandom.domain.ranking.RankSnapshot;
import com.livebroadcast.fandom.domain.ranking.RankingPeriod;
import com.livebroadcast.fandom.domain.ranking.RankingRow;

public interface RankingSnapshotPort {

    void save(Long leagueId, RankingPeriod period, String periodKey, List<RankingRow> rows);

    /** targetId -> rank */
    Map<Long, Integer> findRanks(Long leagueId, RankingPeriod period, String periodKey);

    List<RankSnapshot> findHistory(Long targetId, RankingPeriod period, int limit);
}
