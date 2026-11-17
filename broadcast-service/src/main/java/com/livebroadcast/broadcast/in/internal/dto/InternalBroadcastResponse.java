package com.livebroadcast.broadcast.in.internal.dto;

import com.livebroadcast.broadcast.domain.Broadcast;
import com.livebroadcast.broadcast.domain.BroadcastStatus;
import com.livebroadcast.broadcast.domain.FanSide;

/**
 * interaction-service 가 후원 대상(방송자)과 편파방 응원팀을 확인할 때 쓰는 응답.
 * 편파방 후원은 방송자 수익이 되면서 응원팀 랭킹 점수에도 들어간다.
 */
public record InternalBroadcastResponse(Long id, Long ownerId, String ownerNickname, String title,
                                        BroadcastStatus status, Long targetId, String targetName,
                                        String leagueCode) {

    public static InternalBroadcastResponse from(Broadcast b) {
        FanSide side = b.getSide();
        return new InternalBroadcastResponse(b.getId(), b.getOwnerId(), b.getOwnerNickname(), b.getTitle(),
                b.getStatus(), side.targetId(), side.targetName(), side.leagueCode());
    }
}
