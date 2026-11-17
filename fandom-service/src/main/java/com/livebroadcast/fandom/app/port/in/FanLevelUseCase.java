package com.livebroadcast.fandom.app.port.in;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;

import com.livebroadcast.fandom.domain.level.FanProfile;

public interface FanLevelUseCase {

    FanProfile profile(Long userId);

    /** 채팅 닉네임 옆 등급 표시용(최대 100명) */
    Map<Long, FanProfile> profiles(Collection<Long> userIds);

    /** 하루치 하트를 팬 포인트로 적립(새벽 배치). 적립된 사용자 수 */
    int awardDailyHearts(LocalDate date);
}
