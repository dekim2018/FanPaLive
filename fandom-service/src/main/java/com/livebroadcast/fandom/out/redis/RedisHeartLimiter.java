package com.livebroadcast.fandom.out.redis;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import com.livebroadcast.fandom.app.FandomProperties;
import com.livebroadcast.fandom.app.port.out.HeartLimiterPort;
import com.livebroadcast.fandom.domain.heart.HeartPolicy;
import com.livebroadcast.fandom.domain.heart.LimitDecision;

/**
 * 하트 한도를 Lua 스크립트 한 번으로 검사하고 차감한다(왕복 1회, 원자적).
 * DB 잠금으로 막으면 같은 사용자 행을 잡느라 경합이 생기고, 낙관적 잠금은 연타 시 재시도가 폭증한다.
 */
@Component
public class RedisHeartLimiter implements HeartLimiterPort {

    private final StringRedisTemplate redis;
    @SuppressWarnings("rawtypes")
    private final DefaultRedisScript<List> script;
    private final ZoneId zone;
    private final Clock clock;

    @SuppressWarnings("rawtypes")
    public RedisHeartLimiter(StringRedisTemplate redis, DefaultRedisScript<List> heartLimitScript,
                             FandomProperties properties, Clock clock) {
        this.redis = redis;
        this.script = heartLimitScript;
        this.zone = properties.periodKeys().zone();
        this.clock = clock;
    }

    @Override
    @SuppressWarnings("unchecked")
    public LimitDecision tryConsume(Long userId, Long targetId, LocalDate date, int amount, HeartPolicy policy) {
        List<Long> result = redis.execute(script,
                List.of(RedisKeys.heartDaily(date, userId), RedisKeys.heartDailyTarget(date, userId, targetId)),
                String.valueOf(amount), String.valueOf(policy.dailyMax()), String.valueOf(policy.perTargetMax()),
                String.valueOf(ttlSeconds(date)));
        if (result == null || result.size() < 3) {
            throw new IllegalStateException("heart limit script returned no result");
        }
        return new LimitDecision(result.get(0) == 1L, result.get(1).intValue(), result.get(2).intValue());
    }

    @Override
    public void refund(Long userId, Long targetId, LocalDate date, int amount) {
        redis.opsForValue().decrement(RedisKeys.heartDaily(date, userId), amount);
        redis.opsForValue().decrement(RedisKeys.heartDailyTarget(date, userId, targetId), amount);
    }

    @Override
    public int remainingToday(Long userId, LocalDate date, HeartPolicy policy) {
        String used = redis.opsForValue().get(RedisKeys.heartDaily(date, userId));
        int usedCount = used == null ? 0 : Integer.parseInt(used);
        return Math.max(0, policy.dailyMax() - usedCount);
    }

    /** 키는 그날 KST 자정 1시간 뒤에 사라진다. */
    private long ttlSeconds(LocalDate date) {
        long seconds = Duration.between(clock.instant(), date.plusDays(1).atStartOfDay(zone).toInstant()).toSeconds();
        return Math.max(60, seconds + 3600);
    }
}
