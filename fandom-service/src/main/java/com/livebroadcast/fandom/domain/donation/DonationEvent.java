package com.livebroadcast.fandom.domain.donation;

import java.time.Instant;

/**
 * interaction-service 가 후원 확정 후 Redis Stream 으로 보내는 이벤트.
 * eventId 는 후원 ID 로 만들어서, 같은 이벤트가 두 번 와도(재전송, 재처리) 한 번만 반영된다.
 */
public record DonationEvent(String eventId, Long targetId, Long donorId, String donorNickname, long amount,
                            Instant approvedAt) {
}
