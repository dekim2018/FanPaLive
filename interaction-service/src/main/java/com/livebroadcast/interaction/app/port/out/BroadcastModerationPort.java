package com.livebroadcast.interaction.app.port.out;

/** broadcast-service 에 운영 제재(방송 종료와 숨김)를 요청한다. */
public interface BroadcastModerationPort {

    /** @return 방송 중이어서 이번에 종료됐으면 true */
    boolean moderate(Long broadcastId, String reason);
}
