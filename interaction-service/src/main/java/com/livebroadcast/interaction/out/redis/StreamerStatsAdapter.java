package com.livebroadcast.interaction.out.redis;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.livebroadcast.interaction.app.port.out.StreamerStatsPort;

/**
 * 스트리머 순위 재료.
 * - fanpa:uv:streamer:{팀}:{월}:{스트리머}  순시청자 HLL (키당 최대 12KB)
 * - fanpa:streamers:{팀}:{월}             그 달 그 팀 편파방을 연 스트리머 집합
 * - fanpa:streamer-names                  스트리머 닉네임(마지막 값)
 * 후원 합계는 결제 원본인 DB 에서 바로 합친다(확정 후원만).
 */
@Component
public class StreamerStatsAdapter implements StreamerStatsPort {

    private static final Duration TTL = Duration.ofDays(400);
    private static final String NAMES = "fanpa:streamer-names";

    private final StringRedisTemplate redis;
    private final JdbcTemplate jdbc;

    public StreamerStatsAdapter(StringRedisTemplate redis, JdbcTemplate jdbc) {
        this.redis = redis;
        this.jdbc = jdbc;
    }

    @Override
    public void recordView(Long targetId, String monthKey, Long streamerId, String nickname, String viewerKey) {
        String hll = hllKey(targetId, monthKey, streamerId);
        String set = setKey(targetId, monthKey);
        redis.opsForHyperLogLog().add(hll, viewerKey);
        redis.expire(hll, TTL);
        redis.opsForSet().add(set, String.valueOf(streamerId));
        redis.expire(set, TTL);
        if (nickname != null && !nickname.isBlank()) {
            redis.opsForHash().put(NAMES, String.valueOf(streamerId), nickname);
        }
    }

    @Override
    public Set<Long> streamers(Long targetId, String monthKey) {
        Set<String> members = redis.opsForSet().members(setKey(targetId, monthKey));
        Set<Long> ids = new HashSet<>();
        if (members != null) {
            for (String m : members) {
                try {
                    ids.add(Long.valueOf(m));
                } catch (NumberFormatException ignored) {
                    // 형식이 깨진 값은 건너뛴다.
                }
            }
        }
        return ids;
    }

    @Override
    public Map<Long, Long> uniqueViewers(Long targetId, String monthKey, Collection<Long> streamerIds) {
        Map<Long, Long> result = new HashMap<>();
        for (Long id : streamerIds) {
            Long size = redis.opsForHyperLogLog().size(hllKey(targetId, monthKey, id));
            result.put(id, size == null ? 0 : size);
        }
        return result;
    }

    @Override
    public Map<Long, String> nicknames(Collection<Long> streamerIds) {
        Map<Long, String> result = new HashMap<>();
        if (streamerIds.isEmpty()) {
            return result;
        }
        List<Object> keys = new ArrayList<>(streamerIds.stream().map(String::valueOf).toList());
        List<Object> values = redis.opsForHash().multiGet(NAMES, keys);
        int i = 0;
        for (Long id : streamerIds) {
            Object v = values == null || i >= values.size() ? null : values.get(i);
            if (v != null) {
                result.put(id, v.toString());
            }
            i++;
        }
        return result;
    }

    @Override
    public Map<Long, Long> donations(Long targetId, Instant from, Instant to) {
        Map<Long, Long> result = new HashMap<>();
        jdbc.query("""
                SELECT streamer_id, MAX(streamer_nickname) AS nickname, SUM(amount) AS total
                FROM donations
                WHERE status = 'DONE' AND target_type = 'BROADCAST' AND target_id = ?
                  AND approved_at >= ? AND approved_at < ?
                GROUP BY streamer_id
                """, rs -> {
            long id = rs.getLong("streamer_id");
            result.put(id, rs.getLong("total"));
            String nickname = rs.getString("nickname");
            if (nickname != null) {
                // 시청 기록이 없는 스트리머도 이름이 보이게 한다.
                redis.opsForHash().putIfAbsent(NAMES, String.valueOf(id), nickname);
            }
        }, targetId, Timestamp.from(from), Timestamp.from(to));
        return result;
    }

    private static String hllKey(Long targetId, String monthKey, Long streamerId) {
        return "fanpa:uv:streamer:" + targetId + ":" + monthKey + ":" + streamerId;
    }

    private static String setKey(Long targetId, String monthKey) {
        return "fanpa:streamers:" + targetId + ":" + monthKey;
    }
}
