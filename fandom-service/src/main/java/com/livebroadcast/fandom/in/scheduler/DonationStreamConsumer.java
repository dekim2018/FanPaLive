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

import com.livebroadcast.fandom.app.port.in.DonationEventUseCase;
import com.livebroadcast.fandom.domain.donation.DonationEvent;

/**
 * interaction-service 가 Redis Stream 에 남긴 후원 확정 이벤트를 소비 그룹으로 읽어 집계에 반영한다.
 *
 * - 처리에 성공한 메시지만 ACK 한다. 처리 중 서버가 죽으면 메시지가 대기 목록(PEL)에 남고,
 *   다음 실행 때 내 대기 목록(ReadOffset 0)부터 다시 읽어 처리한다.
 * - 같은 메시지를 두 번 처리해도 DB 의 event_id PK 가 한 번만 반영되게 막는다(멱등 소비자).
 * - 그래도 이벤트가 아예 발행되지 못한 경우(커밋 직후 서버 종료 등)는 새벽 대사 작업이 결제 원본 기준으로 맞춘다.
 */
@Component
public class DonationStreamConsumer {

    private static final Logger log = LoggerFactory.getLogger(DonationStreamConsumer.class);
    public static final String GROUP = "fandom";

    private final StringRedisTemplate redis;
    private final DonationEventUseCase donations;
    private final String stream;
    private final String consumerName;
    private volatile boolean groupReady;

    public DonationStreamConsumer(StringRedisTemplate redis, DonationEventUseCase donations,
                                  @Value("${app.streams.donations:fanpa:stream:donations}") String stream,
                                  @Value("${HOSTNAME:fandom-1}") String consumerName) {
        this.redis = redis;
        this.donations = donations;
        this.stream = stream;
        this.consumerName = consumerName;
    }

    @Scheduled(fixedDelay = 1000, initialDelay = 5000)
    public void poll() {
        try {
            ensureGroup();
            // 1) 처리하다 만 내 메시지 2) 새 메시지 순서로 읽는다.
            consume(ReadOffset.from("0"));
            consume(ReadOffset.lastConsumed());
        } catch (RuntimeException e) {
            groupReady = false;
            log.warn("후원 이벤트 소비 실패(다음 주기에 재시도) reason={}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 3_600_000, initialDelay = 60_000)
    public void trim() {
        try {
            // 처리된 이벤트는 DB 에 남아 있으므로 스트림은 최근 1만 건만 둔다.
            redis.opsForStream().trim(stream, 10_000, true);
        } catch (RuntimeException e) {
            log.debug("스트림 정리 실패 reason={}", e.getMessage());
        }
    }

    private void consume(ReadOffset offset) {
        List<MapRecord<String, Object, Object>> records = redis.opsForStream().read(
                Consumer.from(GROUP, consumerName),
                StreamReadOptions.empty().count(100),
                StreamOffset.create(stream, offset));
        if (records == null) {
            return;
        }
        for (MapRecord<String, Object, Object> record : records) {
            Map<Object, Object> v = record.getValue();
            try {
                DonationEvent event = new DonationEvent(
                        String.valueOf(v.get("eventId")),
                        Long.valueOf(String.valueOf(v.get("targetId"))),
                        Long.valueOf(String.valueOf(v.get("donorId"))),
                        String.valueOf(v.getOrDefault("donorNickname", "")),
                        Long.parseLong(String.valueOf(v.get("amount"))),
                        Instant.parse(String.valueOf(v.get("approvedAt"))));
                donations.apply(event);
            } catch (NumberFormatException | java.time.format.DateTimeParseException e) {
                // 형식이 깨진 메시지는 다시 읽어도 실패하므로 기록하고 넘긴다.
                log.error("후원 이벤트 형식 오류, 건너뜀 id={} value={}", record.getId(), v);
            }
            redis.opsForStream().acknowledge(stream, GROUP, record.getId());
        }
    }

    /** 스트림이 없을 때도 그룹을 만들 수 있게 MKSTREAM 을 쓴다. 이미 있으면(BUSYGROUP) 그대로 쓴다. */
    private void ensureGroup() {
        if (groupReady) {
            return;
        }
        try {
            redis.execute((RedisCallback<Object>) connection -> connection.execute("XGROUP",
                    bytes("CREATE"), bytes(stream), bytes(GROUP), bytes("0"), bytes("MKSTREAM")));
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
