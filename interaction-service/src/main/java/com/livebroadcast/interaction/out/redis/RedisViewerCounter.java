package com.livebroadcast.interaction.out.redis;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.livebroadcast.interaction.app.port.out.ViewerCounterPort;

/**
 * HyperLogLog 기반 순시청자 수.
 * 시청자 ID 를 Set 에 그대로 넣으면 10만 명일 때 키 하나가 수 MB 가 되지만, HLL 은 최대 12KB 로 고정된다.
 * 대신 약 0.81% 오차가 있는 근사치다. 화면에는 "누적 시청자 약 N명"으로 보여 준다.
 */
@Component
public class RedisViewerCounter implements ViewerCounterPort {

    private static final Duration BROADCAST_TTL = Duration.ofDays(30);
    private static final Duration MONTH_TTL = Duration.ofDays(62);

    private final StringRedisTemplate redis;

    public RedisViewerCounter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void record(Long broadcastId, Long targetId, String monthKey, String viewerKey) {
        String broadcastKey = broadcastKey(broadcastId);
        redis.opsForHyperLogLog().add(broadcastKey, viewerKey);
        redis.expire(broadcastKey, BROADCAST_TTL);
        if (targetId != null) {
            String targetKey = targetKey(targetId, monthKey);
            redis.opsForHyperLogLog().add(targetKey, viewerKey);
            redis.expire(targetKey, MONTH_TTL);
        }
    }

    @Override
    public long uniqueViewers(Long broadcastId) {
        Long size = redis.opsForHyperLogLog().size(broadcastKey(broadcastId));
        return size == null ? 0 : size;
    }

    @Override
    public long targetMonthlyUniqueViewers(Long targetId, String monthKey) {
        Long size = redis.opsForHyperLogLog().size(targetKey(targetId, monthKey));
        return size == null ? 0 : size;
    }

    private static String broadcastKey(Long broadcastId) {
        return "fanpa:uv:broadcast:" + broadcastId;
    }

    private static String targetKey(Long targetId, String monthKey) {
        return "fanpa:uv:target:" + targetId + ":" + monthKey;
    }
}
