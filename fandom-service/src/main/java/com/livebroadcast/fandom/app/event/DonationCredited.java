package com.livebroadcast.fandom.app.event;

/** 후원이 응원 대상 집계에 처음 반영됨. 후원한 팬의 포인트 적립에 쓴다. */
public record DonationCredited(String eventId, Long donorId, String donorNickname, Long targetId, long amount) {
}
