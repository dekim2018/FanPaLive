package com.livebroadcast.interaction.domain.donation;

/** 응원 대상(구단/선수)이 받은 응원금과 편파방 후원 합계 */
public record DonationTargetSummary(Long targetId, long totalAmount, long count) {
}
