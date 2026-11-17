package com.livebroadcast.fandom.domain.heart;

/** Redis 한도 검사 결과. 허용되지 않으면 아무것도 차감되지 않는다. */
public record LimitDecision(boolean allowed, int remainingToday, int remainingForTarget) {
}
