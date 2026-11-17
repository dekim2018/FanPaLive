package com.livebroadcast.fandom.app.event;

/** 경기가 끝났거나 취소됨. 승부 예측 정산에 쓴다. */
public record MatchClosed(Long matchId) {
}
