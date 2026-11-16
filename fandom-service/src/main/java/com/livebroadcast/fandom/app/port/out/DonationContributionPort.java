package com.livebroadcast.fandom.app.port.out;

import com.livebroadcast.fandom.domain.donation.DonationEvent;

public interface DonationContributionPort {

    /** 처음 보는 eventId 면 저장하고 true, 이미 반영한 이벤트면 false (PK 로 중복 반영을 막는다) */
    boolean insertIfAbsent(DonationEvent event);
}
