package com.livebroadcast.broadcast.app.port.in;

import com.livebroadcast.broadcast.domain.Broadcast;

public interface BroadcastCommandUseCase {

    Broadcast create(CreateCommand command);

    Broadcast update(UpdateCommand command);

    void delete(Long broadcastId, Long requesterId);

    /**
     * @param targetId 응원팀(편파방). null 이면 일반 방송
     * @param matchId  경기(매치룸에 노출). targetId 가 그 경기의 팀이어야 한다.
     */
    record CreateCommand(Long ownerId, String ownerNickname, String title, String description, Long targetId,
                         Long matchId) {

        public CreateCommand(Long ownerId, String ownerNickname, String title, String description) {
            this(ownerId, ownerNickname, title, description, null, null);
        }
    }

    /**
     * @param clearTarget true 면 응원팀을 지운다. targetId 가 null 이고 clearTarget 이 false 면 기존 응원팀을 그대로 둔다
     *                    (제목만 바꾸는 요청이 응원팀을 지우지 않도록).
     */
    record UpdateCommand(Long broadcastId, Long requesterId, String title, String description, Long targetId,
                         Long matchId, boolean clearTarget) {

        public UpdateCommand(Long broadcastId, Long requesterId, String title, String description) {
            this(broadcastId, requesterId, title, description, null, null, false);
        }
    }
}
