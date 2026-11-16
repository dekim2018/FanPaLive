package com.livebroadcast.fandom.in.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.livebroadcast.fandom.app.port.in.RankingMaintenanceUseCase;

/**
 * 서버가 뜰 때 실시간 순위표를 DB 기준으로 다시 만든다. Redis 가 비었거나(재시작, 장애) 값이 어긋나도 여기서 복구된다.
 */
@Component
public class RankingWarmup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RankingWarmup.class);

    private final RankingMaintenanceUseCase maintenance;

    public RankingWarmup(RankingMaintenanceUseCase maintenance) {
        this.maintenance = maintenance;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            maintenance.rebuildAll();
            log.info("실시간 순위표를 DB 기준으로 다시 만들었습니다.");
        } catch (RuntimeException e) {
            log.warn("기동 시 순위표 재계산 실패(조회 시 다시 시도) reason={}", e.getMessage());
        }
    }
}
