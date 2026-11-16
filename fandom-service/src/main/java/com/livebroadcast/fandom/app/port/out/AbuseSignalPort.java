package com.livebroadcast.fandom.app.port.out;

import java.time.LocalDate;

import com.livebroadcast.fandom.domain.heart.AbuseSignals;

public interface AbuseSignalPort {

    /**
     * 오늘 이 대상에 하트를 준 계정을 기기와 IP 별로 기록하고, 서로 다른 계정 수를 돌려준다.
     *
     * @param accountAgeDays 가입 후 지난 일수(모르면 -1)
     */
    AbuseSignals record(LocalDate date, Long targetId, Long userId, String deviceKey, String ipKey,
                        long accountAgeDays);
}
