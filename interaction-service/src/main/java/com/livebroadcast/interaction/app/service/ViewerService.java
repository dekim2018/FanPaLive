package com.livebroadcast.interaction.app.service;

import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.livebroadcast.interaction.app.port.out.BroadcastQueryPort;
import com.livebroadcast.interaction.app.port.out.PresencePort;
import com.livebroadcast.interaction.app.port.out.StreamerStatsPort;
import com.livebroadcast.interaction.app.port.out.ViewerCounterPort;
import com.livebroadcast.interaction.domain.broadcast.BroadcastInfo;

/**
 * 시청자 수 두 가지.
 * - 현재 시청자: 지금 실시간 이벤트를 구독 중인 연결 수(정확, 메모리)
 * - 누적 순시청자: 방송별, 응원 대상의 월별 고유 시청자 수(HyperLogLog 근사치)
 */
@Service
public class ViewerService {

    private static final Logger log = LoggerFactory.getLogger(ViewerService.class);
    private static final Pattern VIEWER_KEY = Pattern.compile("^[A-Za-z0-9-]{8,64}$");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ViewerCounterPort counter;
    private final PresencePort presence;
    private final BroadcastQueryPort broadcasts;
    private final StreamerStatsPort streamerStats;
    private final Clock clock;

    public ViewerService(ViewerCounterPort counter, PresencePort presence, BroadcastQueryPort broadcasts,
                         StreamerStatsPort streamerStats, Clock clock) {
        this.counter = counter;
        this.presence = presence;
        this.broadcasts = broadcasts;
        this.streamerStats = streamerStats;
        this.clock = clock;
    }

    /**
     * 시청 시작 기록. 로그인 사용자는 user:{id}, 비로그인은 브라우저가 만든 임의 ID 로 센다.
     * 집계 실패가 시청을 막으면 안 되므로 예외는 삼킨다.
     */
    public void recordView(Long broadcastId, Long userId, String anonymousKey) {
        String key = userId != null ? "u" + userId : sanitize(anonymousKey);
        if (key == null) {
            return;
        }
        try {
            BroadcastInfo info = broadcasts.findBroadcast(broadcastId).orElse(null);
            Long targetId = info == null ? null : info.targetId();
            String month = currentMonth();
            counter.record(broadcastId, targetId, month, key);
            if (targetId != null) {
                // 같은 시청자 키로 "이 팀 편파방을 연 스트리머별" 순시청자도 센다(스트리머 순위용).
                streamerStats.recordView(targetId, month, info.ownerId(), info.ownerNickname(), key);
            }
        } catch (RuntimeException e) {
            log.debug("순시청자 기록 실패 broadcastId={} reason={}", broadcastId, e.getMessage());
        }
    }

    public Map<Long, ViewerStats> stats(List<Long> broadcastIds) {
        Map<Long, ViewerStats> result = new LinkedHashMap<>();
        for (Long id : broadcastIds.stream().distinct().limit(100).toList()) {
            long unique;
            try {
                unique = counter.uniqueViewers(id);
            } catch (RuntimeException e) {
                unique = 0;
            }
            result.put(id, new ViewerStats(presence.currentViewers(id), unique));
        }
        return result;
    }

    public long targetMonthlyUniqueViewers(Long targetId) {
        try {
            return counter.targetMonthlyUniqueViewers(targetId, currentMonth());
        } catch (RuntimeException e) {
            return 0;
        }
    }

    static String sanitize(String key) {
        return key != null && VIEWER_KEY.matcher(key).matches() ? "a" + key : null;
    }

    private String currentMonth() {
        return clock.instant().atZone(KST).format(MONTH);
    }

    public record ViewerStats(int current, long unique) {
    }
}
