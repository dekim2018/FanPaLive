package com.livebroadcast.fandom.app.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livebroadcast.fandom.app.FandomProperties;
import com.livebroadcast.fandom.app.port.in.HallOfFameUseCase;
import com.livebroadcast.fandom.app.port.in.RankingMaintenanceUseCase;
import com.livebroadcast.fandom.app.port.out.CatalogRepositoryPort;
import com.livebroadcast.fandom.app.port.out.HallOfFamePort;
import com.livebroadcast.fandom.app.port.out.HeartLogPort;
import com.livebroadcast.fandom.domain.catalog.League;
import com.livebroadcast.fandom.domain.ranking.PeriodKeys;
import com.livebroadcast.fandom.domain.ranking.RankingPeriod;
import com.livebroadcast.fandom.domain.ranking.RankingRow;
import com.livebroadcast.fandom.domain.ranking.SeasonRecord;
import com.livebroadcast.fandom.domain.ranking.Supporter;
import com.livebroadcast.fandom.domain.target.TargetErrors;

/**
 * 명예의 전당. 시즌이 끝나면(시즌 시작일 새벽) 지난 시즌 최종 순위와 우승팀 MVP 팬을 기록한다.
 * 새 시즌 점수는 기간 키(S2027 등)가 바뀌면서 자동으로 0부터 시작한다.
 */
@Service
public class HallOfFameService implements HallOfFameUseCase {

    private static final Logger log = LoggerFactory.getLogger(HallOfFameService.class);
    static final int TOP_N = 10;
    static final int MAX_SEASONS = 10;

    private final RankingMaintenanceUseCase ranking;
    private final HeartLogPort heartLogs;
    private final HallOfFamePort hall;
    private final CatalogRepositoryPort catalog;
    private final PeriodKeys keys;
    private final Clock clock;

    public HallOfFameService(RankingMaintenanceUseCase ranking, HeartLogPort heartLogs, HallOfFamePort hall,
                             CatalogRepositoryPort catalog, FandomProperties properties, Clock clock) {
        this.ranking = ranking;
        this.heartLogs = heartLogs;
        this.hall = hall;
        this.catalog = catalog;
        this.keys = properties.periodKeys();
        this.clock = clock;
    }

    @Override
    @Transactional
    public int closeSeason(LocalDate date) {
        String seasonKey = keys.key(RankingPeriod.SEASON, date);
        ZoneId zone = keys.zone();
        Instant from = keys.start(RankingPeriod.SEASON, date).atStartOfDay(zone).toInstant();
        Instant to = keys.endExclusive(RankingPeriod.SEASON, date).atStartOfDay(zone).toInstant();
        Map<League, List<RankingRow>> finals = ranking.snapshotSeason(date);
        Instant now = clock.instant();
        finals.forEach((league, rows) -> {
            RankingRow champion = rows.get(0);
            List<Supporter> top = heartLogs.findTopSupporters(champion.targetId(), from, to, 1);
            if (!top.isEmpty()) {
                hall.saveMvp(league.id(), seasonKey, champion.targetId(), top.get(0), now);
            }
            log.info("명예의 전당 기록 league={} season={} 우승={}", league.code(), seasonKey, champion.name());
        });
        return finals.size();
    }

    /** 시즌 시작일이면 지난 시즌을 마감한다(매일 새벽 확인). */
    public boolean closePreviousSeasonIfStarted(LocalDate today) {
        if (!keys.start(RankingPeriod.SEASON, today).equals(today)) {
            return false;
        }
        closeSeason(keys.previous(RankingPeriod.SEASON, today));
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeasonRecord> seasons(String leagueCode) {
        League league = catalog.findLeagueByCode(leagueCode).orElseThrow(TargetErrors::leagueNotFound);
        return hall.findSeasons(league.id(), TOP_N, MAX_SEASONS);
    }
}
