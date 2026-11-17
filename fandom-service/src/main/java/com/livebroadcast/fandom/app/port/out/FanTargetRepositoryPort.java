package com.livebroadcast.fandom.app.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.livebroadcast.fandom.domain.target.FanTarget;

public interface FanTargetRepositoryPort {

    FanTarget save(FanTarget target);

    Optional<FanTarget> findById(Long id);

    List<FanTarget> findByIds(Collection<Long> ids);

    /** 공개 중인 대상만 */
    List<FanTarget> findActiveByLeague(Long leagueId);

    List<FanTarget> findAllActive();

    boolean existsByLeagueAndNormalizedName(Long leagueId, String normalizedName);

    List<FanTarget> search(String keyword, int limit);
}
