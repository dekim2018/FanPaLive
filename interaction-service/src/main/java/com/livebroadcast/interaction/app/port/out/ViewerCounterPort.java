package com.livebroadcast.interaction.app.port.out;

/**
 * 순시청자 수 집계(HyperLogLog). 시청자 ID 를 집합에 다 넣지 않고 키당 최대 12KB 로 근사치를 센다(표준 오차 약 0.81%).
 */
public interface ViewerCounterPort {

    void record(Long broadcastId, Long targetId, String monthKey, String viewerKey);

    long uniqueViewers(Long broadcastId);

    long targetMonthlyUniqueViewers(Long targetId, String monthKey);
}
