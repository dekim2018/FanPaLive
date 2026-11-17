package com.livebroadcast.fandom.app.port.out;

import java.util.Collection;

/** 접속 중인 사용자에게 실시간으로 알린다(SSE). 접속하지 않은 사용자는 다음 접속 때 목록에서 본다. */
public interface NotificationPushPort {

    void pushNew(Collection<Long> userIds, String title, String body, String link);

    boolean isOnline(Long userId);
}
