package com.livebroadcast.interaction.app.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.livebroadcast.interaction.app.port.out.DonationEventStreamPort;
import com.livebroadcast.interaction.domain.donation.Donation;
import com.livebroadcast.interaction.domain.donation.DonationCompleted;

/**
 * 확정된 후원 중 응원 대상(구단/선수) 점수에 들어가는 것을 팬덤 서비스용 스트림에 남긴다.
 *
 * 커밋 뒤에 보내므로 롤백된 후원은 나가지 않는다. 반대로 커밋 직후 서버가 죽거나 Redis 가 잠깐 끊겨 못 보낸 건은
 * 팬덤 서비스의 새벽 대사(결제 원본 합계와 비교)가 메운다. 그래서 여기서는 재시도 큐를 따로 두지 않는다.
 */
@Component
public class DonationStreamRelay {

    private static final Logger log = LoggerFactory.getLogger(DonationStreamRelay.class);

    private final DonationEventStreamPort stream;

    public DonationStreamRelay(DonationEventStreamPort stream) {
        this.stream = stream;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(DonationCompleted event) {
        Donation d = event.donation();
        if (!d.countsForFanTarget()) {
            return;
        }
        try {
            stream.publish(d);
        } catch (RuntimeException e) {
            log.warn("후원 이벤트 발행 실패(새벽 대사로 보정됨) orderId={} reason={}", d.getOrderId(), e.getMessage());
        }
    }
}
