package com.livebroadcast.interaction.in.web.dto;

import com.livebroadcast.interaction.domain.donation.DonationTargetType;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * @param targetType 비우면 BROADCAST(방송자 후원). FAN_TARGET 이면 targetId 의 구단/선수 응원기금
 */
public record DonationOrderRequest(
        DonationTargetType targetType,
        Long broadcastId,
        Long targetId,
        @Positive(message = "후원 금액을 입력해 주세요.") long amount,
        @Size(max = 200, message = "후원 메시지는 200자 이하여야 합니다.") String message
) {

    public DonationTargetType typeOrDefault() {
        return targetType == null ? DonationTargetType.BROADCAST : targetType;
    }
}
