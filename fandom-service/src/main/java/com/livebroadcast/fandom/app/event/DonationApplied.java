package com.livebroadcast.fandom.app.event;

import java.time.Instant;

/** 후원이 집계에 처음 반영됨. 커밋 뒤에 실시간 순위표에 반영한다. */
public record DonationApplied(Long leagueId, Long targetId, long points, Instant approvedAt) {
}
