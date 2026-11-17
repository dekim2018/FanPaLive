package com.livebroadcast.broadcast.app.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.livebroadcast.broadcast.app.port.out.LiveEventPort;
import com.livebroadcast.broadcast.domain.event.BroadcastStreamStarted;

/**
 * 편파방에 영상이 실제로 들어오기 시작하면(커밋 뒤) 응원팀 알림용 이벤트를 남긴다.
 * 버튼만 누르고 송출이 연결되지 않은 방송은 알리지 않는다.
 * 송출이 끊겼다 다시 붙어도 이벤트 ID 가 "방송 시작 시각" 기준이라 알림은 한 번만 만들어진다.
 * 알림은 부가 기능이라 이벤트를 남기지 못해도 방송 시작은 그대로 진행한다.
 */
@Component
public class LiveNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(LiveNotificationListener.class);

    private final LiveEventPort liveEvents;

    public LiveNotificationListener(LiveEventPort liveEvents) {
        this.liveEvents = liveEvents;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(BroadcastStreamStarted event) {
        if (!event.broadcast().getSide().isPresent() || event.broadcast().getStartedAt() == null) {
            return;
        }
        try {
            liveEvents.broadcastLive(event.broadcast());
        } catch (RuntimeException e) {
            log.warn("편파방 시작 이벤트 기록 실패 broadcastId={} reason={}", event.broadcast().getId(), e.getMessage());
        }
    }
}
