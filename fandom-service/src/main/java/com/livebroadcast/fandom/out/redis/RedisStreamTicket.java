package com.livebroadcast.fandom.out.redis;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.livebroadcast.fandom.app.port.out.StreamTicketPort;

/** 60초짜리 1회용 구독 티켓. GETDEL 로 꺼내므로 같은 티켓으로 두 번 연결할 수 없다. */
@Component
public class RedisStreamTicket implements StreamTicketPort {

    private static final Duration TTL = Duration.ofSeconds(60);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;

    public RedisStreamTicket(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public String issue(Long userId) {
        byte[] raw = new byte[24];
        RANDOM.nextBytes(raw);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        redis.opsForValue().set(RedisKeys.streamTicket(ticket), String.valueOf(userId), TTL);
        return ticket;
    }

    @Override
    public Optional<Long> redeem(String ticket) {
        String value = redis.opsForValue().getAndDelete(RedisKeys.streamTicket(ticket));
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.valueOf(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
