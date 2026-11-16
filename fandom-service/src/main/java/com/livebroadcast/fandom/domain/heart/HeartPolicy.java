package com.livebroadcast.fandom.domain.heart;

/**
 * 하트 지급 한도. 로그인 사용자는 하루 dailyMax 개를 받고, 한 대상에는 하루 perTargetMax 개까지만 줄 수 있다.
 * 한도는 KST 자정에 다시 채워진다.
 */
public record HeartPolicy(int dailyMax, int perTargetMax) {

    public HeartPolicy {
        if (dailyMax <= 0) dailyMax = 5;
        if (perTargetMax <= 0) perTargetMax = 3;
        perTargetMax = Math.min(perTargetMax, dailyMax);
    }

    public void validateAmount(int amount) {
        if (amount < 1 || amount > perTargetMax) {
            throw HeartErrors.invalidAmount(perTargetMax);
        }
    }
}
