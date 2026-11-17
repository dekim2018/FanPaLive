package com.livebroadcast.fandom.out.redis;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import com.livebroadcast.fandom.app.port.out.RankingBoardPort;
import com.livebroadcast.fandom.domain.ranking.RankingScoreCodec;

@Component
public class RedisRankingBoard implements RankingBoardPort {

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> deltaScript;

    public RedisRankingBoard(StringRedisTemplate redis, DefaultRedisScript<Long> rankingDeltaScript) {
        this.redis = redis;
        this.deltaScript = rankingDeltaScript;
    }

    @Override
    public void applyDelta(Long leagueId, String periodKey, Long targetId, long delta, long secondsSinceSeasonStart) {
        redis.execute(deltaScript, List.of(RedisKeys.ranking(leagueId, periodKey)),
                String.valueOf(targetId), String.valueOf(delta), String.valueOf(secondsSinceSeasonStart),
                String.valueOf(RankingScoreCodec.BASE), String.valueOf(RankingScoreCodec.MAX_SCORE));
    }

    @Override
    public Map<Long, Double> read(Long leagueId, String periodKey) {
        Set<TypedTuple<String>> tuples = redis.opsForZSet().rangeWithScores(RedisKeys.ranking(leagueId, periodKey),
                0, -1);
        Map<Long, Double> result = new HashMap<>();
        if (tuples != null) {
            for (TypedTuple<String> t : tuples) {
                if (t.getValue() != null && t.getScore() != null) {
                    result.put(Long.valueOf(t.getValue()), t.getScore());
                }
            }
        }
        return result;
    }

    /** 임시 키에 다 만든 뒤 RENAME 으로 바꿔 끼운다. 재계산 중에도 읽는 쪽은 완성된 순위표만 본다. */
    @Override
    public void replace(Long leagueId, String periodKey, Map<Long, Double> encodedScores, long ttlSeconds) {
        String key = RedisKeys.ranking(leagueId, periodKey);
        if (encodedScores.isEmpty()) {
            redis.delete(key);
            return;
        }
        String tmp = key + ":rebuild";
        Set<TypedTuple<String>> tuples = new HashSet<>();
        encodedScores.forEach((id, score) -> tuples.add(new DefaultTypedTuple<>(String.valueOf(id), score)));
        redis.delete(tmp);
        redis.opsForZSet().add(tmp, tuples);
        redis.expire(tmp, Duration.ofSeconds(ttlSeconds));
        redis.rename(tmp, key);
    }
}
