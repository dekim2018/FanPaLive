package com.livebroadcast.fandom.in.scheduler;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.livebroadcast.fandom.app.port.in.NotificationUseCase;
import com.livebroadcast.fandom.domain.notification.BroadcastLiveEvent;

/**
 * broadcast-service 가 남긴 "편파방 시작" 이벤트를 읽어 응원팀 팬에게 알림을 만든다.
 * 후원 이벤트 소비자와 같은 방식(소비 그룹, 성공한 것만 ACK, 실패하면 대기 목록에서 다시 읽기)이고,
 * 알림 테이블의 (user_id, event_id) 유일 제약이 중복 처리를 막는다.
 * 발행된 지 10분이 지난 이벤트는 알려도 의미가 적어서 건너뛴다(장애 뒤 밀린 메시지 폭주 방지).
 * 기준은 방송 시작 버튼을 누른 시각(startedAt)이 아니라 이벤트 발행 시각(스트림 레코드 ID)이다.
 * 이벤트는 첫 영상이 들어올 때 발행되므로, OBS 연결이 늦어도 알림이 버려지지 않는다.
 */
@Component
public class BroadcastLiveConsumer {

    private static final Logger log = LoggerFactory.getLogger(BroadcastLiveConsumer.class);
    private static final long STALE_SECONDS = 600;

    private final StringRedisTemplate redis;
    private final NotificationUseCase notifications;
    private final String stream;
    private final String consumerName;
    private volatile boolean groupReady;

    public BroadcastLiveConsumer(StringRedisTemplate redis, NotificationUseCase notifications,
                                 @Value("${app.streams.broadcasts:fanpa:stream:broadcasts}") String stream,
                                 @Value("${HOSTNAME:fandom-1}") String consumerName) {
        this.redis = redis;
        this.notifications = notifications;
        this.stream = stream;
        this.consumerName = consumerName;
    }

    @Scheduled(fixedDelay = 1000, initialDelay = 6000)
    public void poll() {
        try {
            ensureGroup();
            consume(ReadOffset.from("0"));
            consume(ReadOffset.lastConsumed());
        } catch (RuntimeException e) {
            groupReady = false;
            log.warn("방송 시작 이벤트 소비 실패(다음 주기에 재시도) reason={}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 3_600_000, initialDelay = 90_000)
    public void trim() {
        try {
            redis.opsForStream().trim(stream, 5_000, true);
        } catch (RuntimeException e) {
            log.debug("스트림 정리 실패 reason={}", e.getMessage());
        }
    }

    private void consume(ReadOffset offset) {
        List<MapRecord<String, Object, Object>> records = redis.opsForStream().read(
                Consumer.from(DonationStreamConsumer.GROUP, consumerName),
                StreamReadOptions.empty().count(50),
                StreamOffset.create(stream, offset));
        if (records == null) {
            return;
        }
        for (MapRecord<String, Object, Object> record : records) {
            Map<Object, Object> v = record.getValue();
            try {
                BroadcastLiveEvent event = new BroadcastLiveEvent(
                        String.valueOf(v.get("eventId")),
                        Long.valueOf(String.valueOf(v.get("broadcastId"))),
                        Long.valueOf(String.valueOf(v.get("targetId"))),
                        String.valueOf(v.getOrDefault("targetName", "")),
                        String.valueOf(v.getOrDefault("title", "")),
                        Long.valueOf(String.valueOf(v.get("ownerId"))),
                        String.valueOf(v.getOrDefault("ownerNickname", "")),
                        Instant.parse(String.valueOf(v.get("startedAt"))));
                if (isFresh(record, event, Instant.now())) {
                    notifications.onBroadcastLive(event);
                }
            } catch (NumberFormatException | java.time.format.DateTimeParseException e) {
                log.error("방송 시작 이벤트 형식 오류, 건너뜀 id={} value={}", record.getId(), v);
            }
            redis.opsForStream().acknowledge(stream, DonationStreamConsumer.GROUP, record.getId());
        }
    }

    /** 스트림 레코드 ID 의 앞부분은 발행 시각(밀리초)이다. 직접 지정한 ID 라 시각이 없으면 startedAt 으로 대신한다. */
    static boolean isFresh(MapRecord<String, Object, Object> record, BroadcastLiveEvent event, Instant now) {
        Long millis = record.getId().getTimestamp();
        Instant publishedAt = millis != null ? Instant.ofEpochMilli(millis) : event.startedAt();
        return publishedAt.isAfter(now.minusSeconds(STALE_SECONDS));
    }

    private void ensureGroup() {
        if (groupReady) {
            return;
        }
        try {
            redis.execute((RedisCallback<Object>) connection -> connection.execute("XGROUP",
                    bytes("CREATE"), bytes(stream), bytes(DonationStreamConsumer.GROUP), bytes("0"),
                    bytes("MKSTREAM")));
        } catch (RuntimeException e) {
            String message = String.valueOf(e.getMessage()) + String.valueOf(e.getCause());
            if (!message.contains("BUSYGROUP")) {
                throw e;
            }
        }
        groupReady = true;
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
