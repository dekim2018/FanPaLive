package com.livebroadcast.fandom.out.event;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.livebroadcast.fandom.app.port.out.NotificationPushPort;

/**
 * 로그인 사용자별 알림 SSE 연결. 탭을 여러 개 열 수 있어 사용자당 연결을 최대 5개까지 둔다.
 * 연결이 없는 사용자는 아무것도 보내지 않는다(알림은 DB 에 있으니 다음에 목록으로 본다).
 */
@Component
public class NotificationHub implements NotificationPushPort {

    private static final long TIMEOUT_MS = 60 * 60 * 1000L;
    private static final int MAX_PER_USER = 5;
    private static final int MAX_TOTAL = 20_000;

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId, long unreadCount) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        if (connectionCount() >= MAX_TOTAL) {
            emitter.complete();
            return emitter;
        }
        SseEmitter[] evicted = {null};
        emitters.compute(userId, (id, list) -> {
            List<SseEmitter> l = list == null ? new CopyOnWriteArrayList<>() : list;
            if (l.size() >= MAX_PER_USER) {
                // 가장 오래된 연결을 빼고 새 연결을 받는다. 닫기는 compute 밖에서 한다(콜백이 같은 키를 다시 건드림).
                evicted[0] = l.removeFirst();
            }
            l.add(emitter);
            return l;
        });
        if (evicted[0] != null) {
            evicted[0].complete();
        }
        Runnable remove = () -> remove(userId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
        send(userId, emitter, "unread", Map.of("count", unreadCount));
        return emitter;
    }

    @Override
    public void pushNew(Collection<Long> userIds, String title, String body, String link) {
        Map<String, Object> payload = Map.of("title", title, "body", body == null ? "" : body,
                "link", link == null ? "" : link);
        for (Long userId : userIds) {
            List<SseEmitter> list = emitters.get(userId);
            if (list == null) {
                continue;
            }
            for (SseEmitter emitter : list) {
                send(userId, emitter, "notification", payload);
            }
        }
    }

    @Override
    public boolean isOnline(Long userId) {
        List<SseEmitter> list = emitters.get(userId);
        return list != null && !list.isEmpty();
    }

    public int connectionCount() {
        return emitters.values().stream().mapToInt(List::size).sum();
    }

    public void heartbeat() {
        emitters.forEach((userId, list) -> {
            for (SseEmitter emitter : list) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (IOException | IllegalStateException e) {
                    remove(userId, emitter);
                }
            }
        });
    }

    private void send(Long userId, SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            remove(userId, emitter);
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        emitters.computeIfPresent(userId, (id, list) -> {
            list.remove(emitter);
            return list.isEmpty() ? null : list;
        });
    }
}
