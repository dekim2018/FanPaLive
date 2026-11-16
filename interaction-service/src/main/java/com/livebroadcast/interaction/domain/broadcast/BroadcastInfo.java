package com.livebroadcast.interaction.domain.broadcast;

/**
 * broadcast-service 에서 조회한 방송 정보(읽기 전용 스냅샷).
 *
 * @param targetId 편파방 응원팀. 일반 방송이면 null
 */
public record BroadcastInfo(Long id, Long ownerId, String ownerNickname, String title, String status, Long targetId,
                            String targetName) {

    public BroadcastInfo(Long id, Long ownerId, String ownerNickname, String title, String status) {
        this(id, ownerId, ownerNickname, title, status, null, null);
    }
}
