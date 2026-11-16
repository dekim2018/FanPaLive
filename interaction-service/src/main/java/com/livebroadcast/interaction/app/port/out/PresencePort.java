package com.livebroadcast.interaction.app.port.out;

/** 지금 시청 화면을 열어 둔(실시간 이벤트를 구독 중인) 사람 수 */
public interface PresencePort {

    int currentViewers(Long broadcastId);
}
