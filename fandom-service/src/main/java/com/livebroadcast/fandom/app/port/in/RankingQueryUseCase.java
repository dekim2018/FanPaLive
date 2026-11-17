package com.livebroadcast.fandom.app.port.in;

import java.time.LocalDate;
import java.util.List;

import com.livebroadcast.fandom.domain.catalog.League;
import com.livebroadcast.fandom.domain.ranking.RankSnapshot;
import com.livebroadcast.fandom.domain.ranking.RankingPeriod;
import com.livebroadcast.fandom.domain.ranking.RankingRow;
import com.livebroadcast.fandom.domain.ranking.RankingSort;
import com.livebroadcast.fandom.domain.ranking.Supporter;

public interface RankingQueryUseCase {

    RankingBoard ranking(String leagueCode, RankingPeriod period, RankingSort sort);

    List<OverallRow> overall(int limit);

    /** 이번 주 순위. 리그 안에 없으면 null */
    Integer weeklyRank(Long targetId);

    List<RankSnapshot> history(Long targetId, int limit);

    List<Supporter> supporters(Long targetId, RankingPeriod period, int limit);

    record RankingBoard(League league, RankingPeriod period, String periodKey, LocalDate from, LocalDate to,
                        List<RankingRow> rows) {
    }

    record OverallRow(int rank, League league, RankingRow row) {
    }
}
