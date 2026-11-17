package com.livebroadcast.fandom.in.scheduler;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.livebroadcast.fandom.app.FandomProperties;
import com.livebroadcast.fandom.app.port.in.MatchUseCase;
import com.livebroadcast.fandom.app.port.in.RankingMaintenanceUseCase;
import com.livebroadcast.fandom.app.service.HeartAccumulator;
import com.livebroadcast.fandom.out.event.MatchEventHub;

import jakarta.annotation.PreDestroy;

/**
 * 팬덤 서비스의 주기 작업. 시각은 모두 KST 기준이다.
 * 이 서비스는 1대만 띄운다는 전제다. 여러 대로 늘리면 일일/주간 작업은 한 대에서만 돌도록 잠금을 둬야 한다.
 */
@Component
public class FandomSchedulers {

    private static final Logger log = LoggerFactory.getLogger(FandomSchedulers.class);

    private final HeartAccumulator accumulator;
    private final RankingMaintenanceUseCase maintenance;
    private final MatchUseCase matches;
    private final MatchEventHub matchHub;
    private final ZoneId zone;
    private final Clock clock;

    public FandomSchedulers(HeartAccumulator accumulator, RankingMaintenanceUseCase maintenance,
                            MatchUseCase matches, MatchEventHub matchHub, FandomProperties properties, Clock clock) {
        this.accumulator = accumulator;
        this.maintenance = maintenance;
        this.matches = matches;
        this.matchHub = matchHub;
        this.zone = properties.periodKeys().zone();
        this.clock = clock;
    }

    /** 하트 쓰기 병합: 200ms 마다 모아서 반영 */
    @Scheduled(fixedDelay = 200)
    public void flushHearts() {
        accumulator.flush();
    }

    /** 응원 게이지 병합 전송: 500ms 마다 바뀐 경기만 */
    @Scheduled(fixedDelay = 500)
    public void pushMatchGauges() {
        for (Long matchId : matchHub.activeMatches()) {
            try {
                matchHub.publishIfChanged(matchId, matches.gauge(matchId));
            } catch (RuntimeException e) {
                log.debug("게이지 전송 실패 matchId={} reason={}", matchId, e.getMessage());
            }
        }
    }

    @Scheduled(fixedRate = 20_000)
    public void heartbeat() {
        matchHub.heartbeat();
    }

    /** 날짜가 바뀌면 새 기간의 순위표를 DB 기준으로 만든다(월요일이면 새 주간, 1일이면 새 월간). */
    @Scheduled(cron = "10 0 0 * * *", zone = "Asia/Seoul")
    public void rebuildAtMidnight() {
        maintenance.rebuildAll();
    }

    /** 지난주 최종 순위 기록(순위 변동, 순위 추이용) */
    @Scheduled(cron = "0 5 0 * * MON", zone = "Asia/Seoul")
    public void snapshotWeekly() {
        maintenance.snapshotPreviousWeek();
        log.info("주간 순위 스냅샷 저장 완료");
    }

    /** 새벽 대사: 어제 후원 합계를 결제 원본과 비교해 맞추고, 순위표를 DB 기준으로 다시 만든다. */
    @Scheduled(cron = "0 10 4 * * *", zone = "Asia/Seoul")
    public void reconcileDaily() {
        LocalDate yesterday = LocalDate.now(clock.withZone(zone)).minusDays(1);
        try {
            int fixed = maintenance.reconcileDonations(yesterday);
            log.info("후원 집계 대사 완료 date={} 보정 대상 수={}", yesterday, fixed);
        } catch (RuntimeException e) {
            log.warn("후원 집계 대사 실패(다음 날 다시 시도) date={} reason={}", yesterday, e.getMessage());
        }
        maintenance.rebuildAll();
    }

    @PreDestroy
    public void flushOnShutdown() {
        accumulator.flushAll();
    }
}
