package com.livebroadcast.interaction.app.port.out;

import com.livebroadcast.interaction.domain.donation.Donation;

/** 확정된 후원을 팬덤 서비스가 읽는 이벤트 스트림(Redis Stream)에 남긴다. */
public interface DonationEventStreamPort {

    void publish(Donation donation);
}
