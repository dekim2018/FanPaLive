package com.livebroadcast.fandom.out.event;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.stereotype.Component;

import com.livebroadcast.fandom.domain.match.MatchGauge;

/**
 * 매치룸 응원 게이지 실시간 전송(SSE).
 *
 * 하트마다 보내면 메시지 수가 "하트 수 x 시청자 수"가 된다. 그래서 500ms 마다 경기별로 게이지를 한 번 읽고,
 * 값이 바뀐 경기만 구독자에게 한 번씩 보낸다(중간값은 버리는 병합 전송). 시청자 1명이 받는 메시지는 초당 최대 2개다.
 * 보내다 실패한 연결(닫힌 탭, 느린 클라이언트)은 바로 끊고, 다시 연결하면 최신 값부터 받는다.
 */
@Component
public class MatchEventHub {

    private static final long TIMEOUT_MS = 30 * 60 * 1000L;
    private static final int MAX_SUBSCRIBERS_PER_MATCH = 3000;

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final Map<Long, MatchGauge> lastSent = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long matchId, MatchGauge current) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        boolean[] added = {false};
        emitters.compute(matchId, (id, list) -> {
            List<SseEmitter> l = list == null ? new CopyOnWriteArrayList<>() : list;
            if (l.size() < MAX_SUBSCRIBERS_PER_MATCH) {
                l.add(emitter);
                added[0] = true;
            }
            return l.isEmpty() ? null : l;
        });
        if (!added[0]) {
            emitter.complete();
            return emitter;
        }
        Runnable remove = () -> remove(matchId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
        send(matchId, emitter, current);
        return emitter;
    }

    public Iterable<Long> activeMatches() {
        return emitters.keySet();
    }

    public int subscriberCount() {
        return emitters.values().stream().mapToInt(List::size).sum();
    }

    /** 값이 바뀐 경우에만 보낸다. */
    public void publishIfChanged(Long matchId, MatchGauge gauge) {
        MatchGauge previous = lastSent.put(matchId, gauge);
        if (gauge.equals(previous)) {
            return;
        }
        List<SseEmitter> list = emitters.get(matchId);
        if (list == null) {
            return;
        }
        for (SseEmitter emitter : list) {
            send(matchId, emitter, gauge);
        }
    }

    public void heartbeat() {
        emitters.forEach((matchId, list) -> {
            for (SseEmitter emitter : list) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (IOException | IllegalStateException e) {
                    remove(matchId, emitter);
                }
            }
        });
    }

    private void send(Long matchId, SseEmitter emitter, MatchGauge gauge) {
        try {
            emitter.send(SseEmitter.event().name("gauge").data(Map.of("matchId", matchId, "home", gauge.home(),
                    "away", gauge.away(), "homePercent", gauge.homePercent()), MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            remove(matchId, emitter);
        }
    }

    private void remove(Long matchId, SseEmitter emitter) {
        emitters.computeIfPresent(matchId, (id, list) -> {
            list.remove(emitter);
            if (list.isEmpty()) {
                lastSent.remove(matchId);
                return null;
            }
            return list;
        });
    }
}
