package com.livebroadcast.fandom.app;

import java.time.Duration;
import java.time.MonthDay;
import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.livebroadcast.fandom.domain.fan.PrimaryFanPolicy;
import com.livebroadcast.fandom.domain.heart.AbusePolicy;
import com.livebroadcast.fandom.domain.heart.HeartPolicy;
import com.livebroadcast.fandom.domain.ranking.PeriodKeys;

/**
 * 팬덤 규칙 설정. 하트 한도, 어뷰징 기준, 주 응원팀 변경 제한, 시즌 시작일, 기능 플래그를 환경변수로 바꿀 수 있다.
 */
@ConfigurationProperties(prefix = "app.fandom")
public record FandomProperties(
        int heartDailyMax,
        int heartPerTargetMax,
        int heartBasePoints,
        int newAccountDays,
        int maxAccountsPerDevice,
        int maxAccountsPerIp,
        Duration primaryCooldown,
        String seasonStart,
        String zone,
        Features features
) {
    public FandomProperties {
        if (heartDailyMax <= 0) heartDailyMax = 5;
        if (heartPerTargetMax <= 0) heartPerTargetMax = 3;
        if (heartBasePoints <= 0) heartBasePoints = 10;
        if (newAccountDays <= 0) newAccountDays = 7;
        if (maxAccountsPerDevice <= 0) maxAccountsPerDevice = 2;
        if (maxAccountsPerIp <= 0) maxAccountsPerIp = 5;
        if (primaryCooldown == null) primaryCooldown = Duration.ofDays(30);
        if (seasonStart == null || seasonStart.isBlank()) seasonStart = "03-01";
        if (zone == null || zone.isBlank()) zone = "Asia/Seoul";
        if (features == null) features = new Features(true, true, true);
    }

    /**
     * 기능 플래그. 문제가 생긴 기능만 끄고 나머지는 그대로 운영하기 위해 둔다(재배포 없이 환경변수 + 재시작).
     */
    public record Features(boolean hearts, boolean matches, boolean targetRequests) {
    }

    public HeartPolicy heartPolicy() {
        return new HeartPolicy(heartDailyMax, heartPerTargetMax);
    }

    public AbusePolicy abusePolicy() {
        return new AbusePolicy(heartBasePoints, newAccountDays, maxAccountsPerDevice, maxAccountsPerIp);
    }

    public PrimaryFanPolicy primaryFanPolicy() {
        return new PrimaryFanPolicy(primaryCooldown);
    }

    public PeriodKeys periodKeys() {
        return new PeriodKeys(ZoneId.of(zone), MonthDay.parse("--" + seasonStart));
    }
}
