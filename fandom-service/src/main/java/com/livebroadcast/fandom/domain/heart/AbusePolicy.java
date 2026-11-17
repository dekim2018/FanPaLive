package com.livebroadcast.fandom.domain.heart;

/**
 * 랭킹 어뷰징(다중 계정 몰아주기) 판단 규칙.
 * <ul>
 *     <li>가입 newAccountDays 일 미만 계정의 하트는 절반 점수</li>
 *     <li>하루 동안 같은 기기에서 maxAccountsPerDevice 개를 넘는 계정이 같은 대상에 하트를 주면 의심 하트</li>
 *     <li>하루 동안 같은 IP 에서 maxAccountsPerIp 개를 넘는 계정이 같은 대상에 하트를 주면 의심 하트</li>
 * </ul>
 * IP 기준을 기기보다 느슨하게 두는 이유: 회사, 학교, 경기장 와이파이처럼 많은 사람이 같은 공인 IP 를 쓴다.
 */
public record AbusePolicy(int basePoints, int newAccountDays, int maxAccountsPerDevice, int maxAccountsPerIp) {

    public AbusePolicy {
        if (basePoints <= 0) basePoints = 10;
        if (newAccountDays < 0) newAccountDays = 7;
        if (maxAccountsPerDevice <= 0) maxAccountsPerDevice = 2;
        if (maxAccountsPerIp <= 0) maxAccountsPerIp = 5;
    }

    public static AbusePolicy defaults() {
        return new AbusePolicy(10, 7, 2, 5);
    }

    public HeartWeight weigh(AbuseSignals signals) {
        if (signals.sameDeviceAccounts() > maxAccountsPerDevice) {
            return new HeartWeight(0, true, "SAME_DEVICE_ACCOUNTS=" + signals.sameDeviceAccounts());
        }
        if (signals.sameIpAccounts() > maxAccountsPerIp) {
            return new HeartWeight(0, true, "SAME_IP_ACCOUNTS=" + signals.sameIpAccounts());
        }
        if (signals.accountAgeDays() >= 0 && signals.accountAgeDays() < newAccountDays) {
            return new HeartWeight(basePoints / 2, false, "NEW_ACCOUNT");
        }
        return new HeartWeight(basePoints, false, null);
    }
}
