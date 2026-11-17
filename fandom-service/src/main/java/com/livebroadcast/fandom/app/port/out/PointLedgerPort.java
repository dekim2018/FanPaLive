package com.livebroadcast.fandom.app.port.out;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;

import com.livebroadcast.fandom.domain.level.FanProfile;
import com.livebroadcast.fandom.domain.level.PointReason;

public interface PointLedgerPort {

    /** 같은 (사용자, 이유, 근거) 는 한 번만 쌓인다. 새로 쌓였으면 true */
    boolean award(Long userId, String nickname, Long targetId, PointReason reason, String refKey, int points,
                  Instant now);

    /** 하루치 하트를 사용자별로 합쳐 한 번에 적립한다(의심 하트 제외). 적립된 사용자 수 */
    int awardDailyHearts(LocalDate date, int pointsPerHeart, Instant now);

    FanProfile findProfile(Long userId);

    Map<Long, FanProfile> findProfiles(Collection<Long> userIds);
}
