package com.livebroadcast.fandom.app.port.out;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.livebroadcast.fandom.domain.fan.FanMembership;

public interface FanRepositoryPort {

    Optional<FanMembership> find(Long userId, Long targetId);

    Optional<FanMembership> findPrimary(Long userId, Long leagueId);

    FanMembership save(FanMembership membership);

    void delete(Long userId, Long targetId);

    List<FanMembership> findByUser(Long userId);

    List<FanMembership> findPrimaryByUsers(Collection<Long> userIds);

    /** 이 리그에서 주 응원팀을 마지막으로 정한 시각 */
    Optional<Instant> findLastPrimaryChange(Long userId, Long leagueId);

    void recordPrimaryChange(Long userId, Long leagueId, Instant at);

    /** 같은 사용자의 팬 등록 변경을 직렬화한다(동시에 두 팀을 주 응원팀으로 정하는 경쟁 방지). */
    void lockUser(Long userId);
}
