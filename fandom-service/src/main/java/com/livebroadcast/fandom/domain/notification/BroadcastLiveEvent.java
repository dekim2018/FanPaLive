package com.livebroadcast.fandom.domain.notification;

import java.time.Instant;

/**
 * broadcast-service 가 방송 시작을 알리는 이벤트(Redis Stream).
 * eventId 는 "broadcast-live-{방송ID}-{시작 시각}" 이라 같은 시작이 두 번 와도 알림은 한 번만 만든다.
 */
public record BroadcastLiveEvent(String eventId, Long broadcastId, Long targetId, String targetName, String title,
                                 Long ownerId, String ownerNickname, Instant startedAt) {

    private static final int MAX_TITLE = 60;

    public String notificationTitle() {
        String team = targetName == null || targetName.isBlank() ? "응원팀" : targetName;
        return team + " 편파방이 시작됐어요";
    }

    public String notificationBody() {
        String safeTitle = title == null ? "" : title.strip();
        if (safeTitle.length() > MAX_TITLE) {
            safeTitle = safeTitle.substring(0, MAX_TITLE) + "…";
        }
        return ownerNickname + ": " + safeTitle;
    }

    public String link() {
        return "/broadcasts/" + broadcastId;
    }
}
