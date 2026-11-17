package com.livebroadcast.interaction.in.web.dto;

import java.time.Instant;

import com.livebroadcast.interaction.domain.donation.Donation;
import com.livebroadcast.interaction.domain.donation.DonationStatus;
import com.livebroadcast.interaction.domain.donation.DonationTargetType;

public record DonationResponse(Long id, String orderId, DonationTargetType targetType, Long broadcastId,
                               String streamerNickname, Long targetId, String targetName, String donorNickname,
                               long amount, String message, DonationStatus status, Instant approvedAt) {

    public static DonationResponse from(Donation d) {
        return new DonationResponse(d.getId(), d.getOrderId(), d.getTargetType(), d.getBroadcastId(),
                d.getStreamerNickname(), d.getTargetId(), d.getTargetName(), d.getDonorNickname(), d.getAmount(),
                d.getMessage(), d.getStatus(), d.getApprovedAt());
    }
}
