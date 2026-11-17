package com.livebroadcast.fandom.domain.fan;

import java.time.Instant;

/**
 * 사용자와 응원 대상의 관계. primary 면 그 리그의 주 응원팀(리그당 1개), 아니면 관심 대상(팔로우)이다.
 */
public record FanMembership(Long userId, Long targetId, Long leagueId, String userNickname, boolean primary,
                            Instant joinedAt) {

    public FanMembership asPrimary(boolean value) {
        return new FanMembership(userId, targetId, leagueId, userNickname, value, joinedAt);
    }
}
