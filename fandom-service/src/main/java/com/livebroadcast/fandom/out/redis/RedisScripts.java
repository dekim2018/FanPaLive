package com.livebroadcast.fandom.out.redis;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

/** Lua 스크립트는 resources/scripts 에 두고 SHA 로 캐시해서 실행한다(EVALSHA). */
@Configuration
public class RedisScripts {

    @Bean
    @SuppressWarnings({"rawtypes", "unchecked"})
    public DefaultRedisScript<List> heartLimitScript() {
        return script("scripts/heart_limit.lua", List.class);
    }

    @Bean
    public DefaultRedisScript<Long> rankingDeltaScript() {
        return script("scripts/ranking_delta.lua", Long.class);
    }

    @Bean
    @SuppressWarnings({"rawtypes", "unchecked"})
    public DefaultRedisScript<List> abuseRecordScript() {
        return script("scripts/abuse_record.lua", List.class);
    }

    private static <T> DefaultRedisScript<T> script(String path, Class<T> type) {
        DefaultRedisScript<T> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource(path)));
        script.setResultType(type);
        return script;
    }
}
