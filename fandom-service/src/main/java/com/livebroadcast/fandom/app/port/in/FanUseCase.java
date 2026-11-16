package com.livebroadcast.fandom.app.port.in;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.livebroadcast.fandom.domain.catalog.League;
import com.livebroadcast.fandom.domain.fan.FanMembership;
import com.livebroadcast.fandom.domain.target.FanTarget;

public interface FanUseCase {

    /**
     * 팬 등록 또는 변경. primary=true 면 그 리그의 주 응원팀으로 정하고(기존 주 응원팀은 관심 팬으로 내려감),
     * false 면 관심 팬으로 등록한다(이미 주 응원팀이면 관심 팬으로 내림).
     */
    FanMembership join(Long userId, String nickname, Long targetId, boolean primary);

    void leave(Long userId, Long targetId);

    List<MyFan> myFans(Long userId);

    /** 채팅, 후원 알림 옆에 붙일 주 응원팀 배지 */
    Map<Long, List<Badge>> badges(Collection<Long> userIds);

    record MyFan(FanMembership membership, FanTarget target, League league, Instant primaryChangeAvailableAt) {
    }

    record Badge(Long targetId, String shortName, String colorPrimary, String colorSecondary, String leagueCode) {
    }
}
