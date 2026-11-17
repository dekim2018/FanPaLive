package com.livebroadcast.broadcast.out.event;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.livebroadcast.broadcast.app.port.out.LiveEventPort;
import com.livebroadcast.broadcast.domain.Broadcast;

/** Redis Stream(fanpa:stream:broadcasts)에 편파방 시작 이벤트를 남긴다. fandom-service 가 소비 그룹으로 읽는다. */
@Component
public class RedisLiveEventPublisher implements LiveEventPort {

    private final StringRedisTemplate redis;
    private final String stream;

    public RedisLiveEventPublisher(StringRedisTemplate redis,
                                   @Value("${app.streams.broadcasts:fanpa:stream:broadcasts}") String stream) {
        this.redis = redis;
        this.stream = stream;
    }

    @Override
    public void broadcastLive(Broadcast b) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("eventId", "broadcast-live-" + b.getId() + "-" + b.getStartedAt().getEpochSecond());
        fields.put("broadcastId", String.valueOf(b.getId()));
        fields.put("targetId", String.valueOf(b.getSide().targetId()));
        fields.put("targetName", b.getSide().targetName() == null ? "" : b.getSide().targetName());
        fields.put("title", b.getTitle());
        fields.put("ownerId", String.valueOf(b.getOwnerId()));
        fields.put("ownerNickname", b.getOwnerNickname());
        fields.put("startedAt", b.getStartedAt().toString());
        redis.opsForStream().add(StreamRecords.string(fields).withStreamKey(stream));
    }
}
