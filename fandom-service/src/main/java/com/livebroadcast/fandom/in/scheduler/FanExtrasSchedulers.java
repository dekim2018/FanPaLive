package com.livebroadcast.fandom.in.scheduler;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.livebroadcast.fandom.app.FandomProperties;
import com.livebroadcast.fandom.app.port.in.FanLevelUseCase;
import com.livebroadcast.fandom.app.service.HallOfFameService;
import com.livebroadcast.fandom.app.service.NotificationService;
import com.livebroadcast.fandom.out.event.NotificationHub;

/** 팬 포인트, 명예의 전당, 알림 정리 주기 작업(KST) */
@Component
public class FanExtrasSchedulers {

    private static final Logger log = LoggerFactory.getLogger(FanExtrasSchedulers.class);

    private final FanLevelUseCase levels;
    private final HallOfFameService hallOfFame;
    private final NotificationService notifications;
    private final NotificationHub hub;
    private final ZoneId zone;
    private final Clock clock;

    public FanExtrasSchedulers(FanLevelUseCase levels, HallOfFameService hallOfFame,
                               NotificationService notifications, NotificationHub hub, FandomProperties properties,
                               Clock clock) {
        this.levels = levels;
        this.hallOfFame = hallOfFame;
        this.notifications = notifications;
        this.hub = hub;
        this.zone = properties.periodKeys().zone();
        this.clock = clock;
    }

    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        hub.heartbeat();
    }

    /** 새 시즌이 시작된 첫날, 지난 시즌 최종 순위와 MVP 를 명예의 전당에 기록한다. */
    @Scheduled(cron = "0 20 0 * * *", zone = "Asia/Seoul")
    public void closeSeason() {
        try {
            if (hallOfFame.closePreviousSeasonIfStarted(today())) {
                log.info("지난 시즌 명예의 전당 기록 완료");
            }
        } catch (RuntimeException e) {
            log.warn("명예의 전당 기록 실패(관리자 화면에서 다시 실행 가능) reason={}", e.getMessage());
        }
    }

    /** 어제 준 하트를 팬 포인트로 적립. 원장 유일 제약 덕분에 다시 돌려도 두 번 쌓이지 않는다. */
    @Scheduled(cron = "0 30 0 * * *", zone = "Asia/Seoul")
    public void awardHearts() {
        LocalDate yesterday = today().minusDays(1);
        try {
            log.info("하트 포인트 적립 date={} 사용자 수={}", yesterday, levels.awardDailyHearts(yesterday));
        } catch (RuntimeException e) {
            log.warn("하트 포인트 적립 실패 date={} reason={}", yesterday, e.getMessage());
        }
    }

    @Scheduled(cron = "0 40 4 * * SUN", zone = "Asia/Seoul")
    public void cleanupNotifications() {
        log.info("읽은 지 오래된 알림 정리 건수={}", notifications.cleanup());
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(zone));
    }
}
