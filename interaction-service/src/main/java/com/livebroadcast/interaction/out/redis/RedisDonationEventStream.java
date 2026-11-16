package com.livebroadcast.interaction.out.redis;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.livebroadcast.interaction.app.port.out.DonationEventStreamPort;
import com.livebroadcast.interaction.domain.donation.Donation;

/**
 * 후원 확정 이벤트를 Redis Stream 에 XADD 한다. fandom-service 가 소비 그룹으로 읽는다.
 * eventId 는 후원 ID 로 고정해서, 같은 후원이 두 번 들어가도 소비 쪽에서 한 번만 반영된다.
 */
@Component
public class RedisDonationEventStream implements DonationEventStreamPort {

    private final StringRedisTemplate redis;
    private final String stream;

    public RedisDonationEventStream(StringRedisTemplate redis,
                                    @Value("${app.streams.donations:fanpa:stream:donations}") String stream) {
        this.redis = redis;
        this.stream = stream;
    }

    @Override
    public void publish(Donation d) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("eventId", "donation-" + d.getId());
        fields.put("targetId", String.valueOf(d.getTargetId()));
        fields.put("donorId", String.valueOf(d.getDonorId()));
        fields.put("donorNickname", d.getDonorNickname() == null ? "" : d.getDonorNickname());
        fields.put("amount", String.valueOf(d.getAmount()));
        fields.put("approvedAt", d.getApprovedAt().toString());
        fields.put("type", d.getTargetType().name());
        redis.opsForStream().add(StreamRecords.newRecord().in(stream).ofMap(fields));
    }
}
