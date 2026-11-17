package com.livebroadcast.interaction.in.scheduler;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.livebroadcast.interaction.app.port.in.DonationUseCase;

/**
 * 결제 후속 처리 작업.
 * - 1분마다: 결과를 모르는(UNKNOWN) 주문을 결제 조회로 확정
 * - 매일 03:30: 전날 결제사 거래 내역과 우리 기록 비교
 *
 * 인스턴스를 여러 대로 늘리면 같은 작업이 겹쳐 돌 수 있지만, 주문 단위 행 잠금과 상태 검사로 중복 적립은 생기지 않는다.
 */
@Component
public class PaymentSchedulers {

    private static final Logger log = LoggerFactory.getLogger(PaymentSchedulers.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DonationUseCase donations;
    private final Clock clock;

    public PaymentSchedulers(DonationUseCase donations, Clock clock) {
        this.donations = donations;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void resolveUnknown() {
        try {
            int resolved = donations.resolveUnknown();
            if (resolved > 0) {
                log.info("결과를 모르던 주문 {}건 확정", resolved);
            }
        } catch (RuntimeException e) {
            log.warn("UNKNOWN 주문 확인 실패 reason={}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Seoul")
    public void reconcileYesterday() {
        LocalDate yesterday = LocalDate.now(clock.withZone(KST)).minusDays(1);
        try {
            int fixed = donations.reconcileWithGateway(yesterday);
            log.info("결제 대사 완료 date={} 보정={}건", yesterday, fixed);
        } catch (RuntimeException e) {
            log.warn("결제 대사 실패 date={} reason={}", yesterday, e.getMessage());
        }
    }
}
