package com.livebroadcast.fandom.out.redis;

import java.time.Duration;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.livebroadcast.fandom.app.port.out.MatchGaugePort;
import com.livebroadcast.fandom.domain.match.MatchGauge;
import com.livebroadcast.fandom.domain.match.MatchSide;

@Component
public class RedisMatchGauge implements MatchGaugePort {

    private static final Duration TTL = Duration.ofDays(14);

    private final StringRedisTemplate redis;

    public RedisMatchGauge(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void add(Long matchId, MatchSide side, long hearts) {
        String key = RedisKeys.matchGauge(matchId);
        redis.opsForHash().increment(key, side.name(), hearts);
        redis.expire(key, TTL);
    }

    @Override
    public MatchGauge read(Long matchId) {
        Map<Object, Object> values = redis.opsForHash().entries(RedisKeys.matchGauge(matchId));
        return new MatchGauge(parse(values.get(MatchSide.HOME.name())), parse(values.get(MatchSide.AWAY.name())));
    }

    private static long parse(Object value) {
        return value == null ? 0 : Long.parseLong(value.toString());
    }
}
