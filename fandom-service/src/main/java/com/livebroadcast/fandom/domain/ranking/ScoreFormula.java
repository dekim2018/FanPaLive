package com.livebroadcast.fandom.domain.ranking;

import com.livebroadcast.fandom.domain.fan.FanChange;

/**
 * 응원 점수 = 주 응원팬 x 100 + 관심팬 x 20 + 기간 하트 점수 + 기간 후원금(원) / 100
 * 하트 점수는 하트 1개당 기본 10점(신규 계정 5점, 의심 하트 0점)이다.
 */
public final class ScoreFormula {

    public static final long WON_PER_POINT = 100;

    private ScoreFormula() {
    }

    public static long score(long primaryFans, long followFans, long heartPoints, long donationAmount) {
        return primaryFans * FanChange.PRIMARY_POINTS + followFans * FanChange.FOLLOW_POINTS
                + heartPoints + donationPoints(donationAmount);
    }

    public static long donationPoints(long amount) {
        return Math.max(0, amount) / WON_PER_POINT;
    }
}
