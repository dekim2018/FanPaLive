package com.livebroadcast.fandom.in.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;

import com.livebroadcast.fandom.domain.notification.BroadcastLiveEvent;

/** 방송 시작 알림의 "오래된 이벤트" 판단은 시작 버튼 시각이 아니라 발행 시각 기준 */
class BroadcastLiveConsumerTest {

    final Instant now = Instant.parse("2026-11-17T09:00:00Z");

    BroadcastLiveEvent startedAt(Instant startedAt) {
        return new BroadcastLiveEvent("broadcast-live-7-1", 7L, 9L, "두산 베어스", "잠실 직관", 3L, "방장", startedAt);
    }

    MapRecord<String, Object, Object> publishedAt(Instant publishedAt) {
        return StreamRecords.<String, Object, Object>mapBacked(Map.of("eventId", "x"))
                .withStreamKey("fanpa:stream:broadcasts")
                .withId(RecordId.of(publishedAt.toEpochMilli(), 0));
    }

    @Test
    void 시작_버튼을_누르고_OBS_연결이_늦어도_방금_발행된_이벤트는_알린다() {
        BroadcastLiveEvent event = startedAt(now.minusSeconds(30 * 60));

        assertThat(BroadcastLiveConsumer.isFresh(publishedAt(now.minusSeconds(5)), event, now)).isTrue();
    }

    @Test
    void 발행된_지_10분이_지난_이벤트는_건너뛴다() {
        BroadcastLiveEvent event = startedAt(now.minusSeconds(12 * 60));

        assertThat(BroadcastLiveConsumer.isFresh(publishedAt(now.minusSeconds(11 * 60)), event, now)).isFalse();
    }
}
