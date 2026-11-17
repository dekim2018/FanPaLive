package com.livebroadcast.broadcast.app.port.out;

import com.livebroadcast.broadcast.domain.Broadcast;

/** 편파방이 시작됐다는 사실을 다른 서비스(fandom-service 의 응원팀 알림)에 알린다. */
public interface LiveEventPort {

    void broadcastLive(Broadcast broadcast);
}
