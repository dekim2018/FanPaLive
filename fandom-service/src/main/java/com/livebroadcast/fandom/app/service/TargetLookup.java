package com.livebroadcast.fandom.app.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.livebroadcast.fandom.app.port.out.CatalogRepositoryPort;
import com.livebroadcast.fandom.app.port.out.FanTargetRepositoryPort;
import com.livebroadcast.fandom.domain.catalog.League;
import com.livebroadcast.fandom.domain.target.FanTarget;
import com.livebroadcast.fandom.domain.target.TargetErrors;

/**
 * 하트처럼 요청이 몰리는 경로에서 대상과 리그를 매번 DB 에서 읽지 않도록 짧게 캐시한다.
 * 대상은 거의 바뀌지 않고, 숨김 처리가 30초 늦게 반영되는 정도는 허용한다.
 */
@Component
public class TargetLookup {

    private static final Duration TTL = Duration.ofSeconds(30);

    private final FanTargetRepositoryPort targets;
    private final CatalogRepositoryPort catalog;
    private final Clock clock;
    private final Map<Long, Cached<FanTarget>> targetCache = new ConcurrentHashMap<>();
    private final Map<Long, Cached<League>> leagueCache = new ConcurrentHashMap<>();

    public TargetLookup(FanTargetRepositoryPort targets, CatalogRepositoryPort catalog, Clock clock) {
        this.targets = targets;
        this.catalog = catalog;
        this.clock = clock;
    }

    public FanTarget target(Long targetId) {
        Instant now = clock.instant();
        Cached<FanTarget> cached = targetCache.get(targetId);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.value();
        }
        FanTarget target = targets.findById(targetId).orElseThrow(TargetErrors::notFound);
        targetCache.put(targetId, new Cached<>(target, now.plus(TTL)));
        return target;
    }

    public FanTarget visibleTarget(Long targetId) {
        FanTarget target = target(targetId);
        target.ensureVisible();
        return target;
    }

    public League league(Long leagueId) {
        Instant now = clock.instant();
        Cached<League> cached = leagueCache.get(leagueId);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.value();
        }
        League league = catalog.findLeagueById(leagueId).orElseThrow(TargetErrors::leagueNotFound);
        leagueCache.put(leagueId, new Cached<>(league, now.plus(TTL)));
        return league;
    }

    public void evict(Long targetId) {
        targetCache.remove(targetId);
    }

    private record Cached<T>(T value, Instant expiresAt) {
    }
}
