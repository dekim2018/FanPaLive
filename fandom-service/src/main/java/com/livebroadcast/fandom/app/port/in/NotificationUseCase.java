package com.livebroadcast.fandom.app.port.in;

import java.util.Collection;

import com.livebroadcast.fandom.app.PageResult;
import com.livebroadcast.fandom.domain.notification.BroadcastLiveEvent;
import com.livebroadcast.fandom.domain.notification.Notification;

public interface NotificationUseCase {

    NotificationPage list(Long userId, int page, int size);

    long unreadCount(Long userId);

    int markRead(Long userId, Collection<Long> ids);

    /** 응원팀 편파방 시작 이벤트 처리. 알림을 받은 사람 수 */
    int onBroadcastLive(BroadcastLiveEvent event);

    String issueStreamTicket(Long userId);

    Long redeemStreamTicket(String ticket);

    record NotificationPage(PageResult<Notification> page, long unreadCount) {
    }
}
