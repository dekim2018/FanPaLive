package com.livebroadcast.fandom.domain.heart;

/**
 * 하트 한 건을 판단할 때 쓰는 신호.
 *
 * @param accountAgeDays    가입 후 지난 일수. 모르면 -1
 * @param sameDeviceAccounts 오늘 같은 기기에서 이 대상에 하트를 준 서로 다른 계정 수(이번 계정 포함)
 * @param sameIpAccounts     오늘 같은 IP 에서 이 대상에 하트를 준 서로 다른 계정 수(이번 계정 포함)
 */
public record AbuseSignals(long accountAgeDays, long sameDeviceAccounts, long sameIpAccounts) {

    public static AbuseSignals none() {
        return new AbuseSignals(-1, 1, 1);
    }
}
