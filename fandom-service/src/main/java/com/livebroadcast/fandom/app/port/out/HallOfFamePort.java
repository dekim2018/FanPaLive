package com.livebroadcast.fandom.app.port.out;

import java.time.Instant;
import java.util.List;

import com.livebroadcast.fandom.domain.ranking.SeasonRecord;
import com.livebroadcast.fandom.domain.ranking.Supporter;

public interface HallOfFamePort {

    void saveMvp(Long leagueId, String seasonKey, Long targetId, Supporter mvp, Instant now);

    /** 시즌 최종 순위(ranking_snapshots 의 SEASON)와 MVP 를 최신 시즌부터 */
    List<SeasonRecord> findSeasons(Long leagueId, int topN, int seasons);
}
