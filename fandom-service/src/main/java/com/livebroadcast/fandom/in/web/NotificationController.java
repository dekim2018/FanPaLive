package com.livebroadcast.fandom.in.web;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.livebroadcast.common.security.AuthUser;
import com.livebroadcast.common.security.LoginUser;
import com.livebroadcast.fandom.app.port.in.NotificationUseCase;
import com.livebroadcast.fandom.in.web.dto.ExtrasDtos.MarkReadRequest;
import com.livebroadcast.fandom.in.web.dto.ExtrasDtos.NotificationListResponse;
import com.livebroadcast.fandom.in.web.dto.ExtrasDtos.NotificationResponse;
import com.livebroadcast.fandom.in.web.dto.ExtrasDtos.TicketResponse;
import com.livebroadcast.fandom.out.event.NotificationHub;

import jakarta.validation.Valid;

/**
 * 사이트 안 알림. 실시간 구독은 두 단계다.
 * 1) 로그인 상태로 POST /stream-ticket 을 불러 60초짜리 1회용 티켓을 받고
 * 2) EventSource 로 GET /stream?ticket=... 에 연결한다. 이 주소는 토큰 없이 열려 있고 티켓으로만 사용자를 확인한다.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationUseCase notifications;
    private final NotificationHub hub;

    public NotificationController(NotificationUseCase notifications, NotificationHub hub) {
        this.notifications = notifications;
        this.hub = hub;
    }

    @GetMapping
    public NotificationListResponse list(@LoginUser AuthUser user, @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        var result = notifications.list(user.id(), page, size);
        var p = result.page();
        return new NotificationListResponse(p.content().stream().map(NotificationResponse::from).toList(), p.page(),
                p.size(), p.totalElements(), p.totalPages(), result.unreadCount());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread(@LoginUser AuthUser user) {
        return Map.of("count", notifications.unreadCount(user.id()));
    }

    @PostMapping("/read")
    public Map<String, Object> read(@LoginUser AuthUser user, @Valid @RequestBody MarkReadRequest body) {
        List<Long> ids = body.ids() == null ? List.of() : body.ids();
        int changed = notifications.markRead(user.id(), ids);
        return Map.of("changed", changed, "unreadCount", notifications.unreadCount(user.id()));
    }

    @PostMapping("/stream-ticket")
    public TicketResponse ticket(@LoginUser AuthUser user) {
        return new TicketResponse(notifications.issueStreamTicket(user.id()), 60);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String ticket) {
        Long userId = notifications.redeemStreamTicket(ticket);
        return hub.subscribe(userId, notifications.unreadCount(userId));
    }
}
