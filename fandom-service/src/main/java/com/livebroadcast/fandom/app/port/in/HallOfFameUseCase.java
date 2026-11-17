package com.livebroadcast.fandom.app.port.in;

import java.time.LocalDate;
import java.util.List;

import com.livebroadcast.fandom.domain.ranking.SeasonRecord;

public interface HallOfFameUseCase {

    /** date 가 속한 시즌의 최종 순위와 MVP 팬을 기록한다(여러 번 해도 결과는 같다). 기록한 리그 수 */
    int closeSeason(LocalDate date);

    List<SeasonRecord> seasons(String leagueCode);
}
