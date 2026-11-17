package com.livebroadcast.fandom.domain.ranking;

/** 대상에 하트와 후원으로 기여한 팬. points = 하트 점수 + 후원금 / 100 */
public record Supporter(Long userId, String nickname, long points, long hearts, long donationAmount) {
}
