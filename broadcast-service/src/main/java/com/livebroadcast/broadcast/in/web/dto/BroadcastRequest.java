package com.livebroadcast.broadcast.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param targetId 응원팀(편파방). 비우면 일반 방송
 * @param matchId  경기(매치룸에 노출). 응원팀이 그 경기의 팀이어야 한다.
 */
public record BroadcastRequest(
        @NotBlank(message = "방송 제목을 입력해 주세요.")
        @Size(max = 100, message = "방송 제목은 100자 이하여야 합니다.")
        String title,

        @Size(max = 1000, message = "방송 설명은 1000자 이하여야 합니다.")
        String description,

        Long targetId,

        Long matchId,

        /** 수정할 때 응원팀을 지우려면 true. 비우면 기존 응원팀을 그대로 둔다. */
        Boolean clearTarget
) {
}
