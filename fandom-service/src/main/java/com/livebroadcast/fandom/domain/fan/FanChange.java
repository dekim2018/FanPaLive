package com.livebroadcast.fandom.domain.fan;

/**
 * 팬 등록/변경으로 생긴 대상별 팬 수 변화. 랭킹 점수(주 응원팬 100점, 관심팬 20점)와 통계에 그대로 반영한다.
 */
public record FanChange(Long targetId, int primaryDelta, int followDelta) {

    public static final int PRIMARY_POINTS = 100;
    public static final int FOLLOW_POINTS = 20;

    public long scoreDelta() {
        return (long) primaryDelta * PRIMARY_POINTS + (long) followDelta * FOLLOW_POINTS;
    }

    public boolean isEmpty() {
        return primaryDelta == 0 && followDelta == 0;
    }
}
