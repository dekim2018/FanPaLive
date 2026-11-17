package com.livebroadcast.fandom.domain.notification;

import java.time.Instant;

/** 사이트 안 알림 한 건(종 아이콘). */
public record Notification(Long id, Long userId, NotificationType type, String title, String body, String link,
                           Long targetId, Instant createdAt, Instant readAt) {

    public boolean isRead() {
        return readAt != null;
    }
}
