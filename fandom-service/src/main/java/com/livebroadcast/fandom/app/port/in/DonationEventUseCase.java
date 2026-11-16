package com.livebroadcast.fandom.app.port.in;

import com.livebroadcast.fandom.domain.donation.DonationEvent;

public interface DonationEventUseCase {

    /** @return 이번에 처음 반영했으면 true, 이미 반영된 이벤트면 false */
    boolean apply(DonationEvent event);
}
