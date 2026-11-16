package com.livebroadcast.broadcast.app;

import com.livebroadcast.broadcast.domain.BroadcastStatus;

/** 방송 목록 필터. 값이 null 인 조건은 쓰지 않는다. */
public record BroadcastFilter(BroadcastStatus status, String leagueCode, Long targetId, Long matchId) {

    public static BroadcastFilter ofStatus(BroadcastStatus status) {
        return new BroadcastFilter(status, null, null, null);
    }
}
