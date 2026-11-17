package com.livebroadcast.fandom.out.redis;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import com.livebroadcast.fandom.app.port.out.AbuseSignalPort;
import com.livebroadcast.fandom.domain.heart.AbuseSignals;

@Component
public class RedisAbuseSignals implements AbuseSignalPort {

    private static final long TTL_SECONDS = 2 * 86_400;

    private final StringRedisTemplate redis;
    @SuppressWarnings("rawtypes")
    private final DefaultRedisScript<List> script;

    @SuppressWarnings("rawtypes")
    public RedisAbuseSignals(StringRedisTemplate redis, DefaultRedisScript<List> abuseRecordScript) {
        this.redis = redis;
        this.script = abuseRecordScript;
    }

    @Override
    @SuppressWarnings("unchecked")
    public AbuseSignals record(LocalDate date, Long targetId, Long userId, String deviceKey, String ipKey,
                               long accountAgeDays) {
        boolean hasDevice = deviceKey != null;
        boolean hasIp = ipKey != null;
        if (!hasDevice && !hasIp) {
            return new AbuseSignals(accountAgeDays, 1, 1);
        }
        List<Long> result = redis.execute(script,
                List.of(RedisKeys.abuseDevice(date, targetId, hasDevice ? deviceKey : "none"),
                        RedisKeys.abuseIp(date, targetId, hasIp ? ipKey : "none")),
                String.valueOf(userId), String.valueOf(TTL_SECONDS), hasDevice ? "1" : "0", hasIp ? "1" : "0");
        if (result == null || result.size() < 2) {
            return new AbuseSignals(accountAgeDays, 1, 1);
        }
        return new AbuseSignals(accountAgeDays, result.get(0), result.get(1));
    }
}
