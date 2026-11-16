package com.livebroadcast.fandom.app.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livebroadcast.fandom.app.FandomProperties;
import com.livebroadcast.fandom.app.FeatureGuard;
import com.livebroadcast.fandom.app.port.in.MatchUseCase;
import com.livebroadcast.fandom.app.port.out.CatalogRepositoryPort;
import com.livebroadcast.fandom.app.port.out.MatchGaugePort;
import com.livebroadcast.fandom.app.port.out.MatchRepositoryPort;
import com.livebroadcast.fandom.domain.catalog.League;
import com.livebroadcast.fandom.domain.match.Match;
import com.livebroadcast.fandom.domain.match.MatchErrors;
import com.livebroadcast.fandom.domain.match.MatchGauge;
import com.livebroadcast.fandom.domain.target.FanTarget;
import com.livebroadcast.fandom.domain.target.TargetErrors;

@Service
public class MatchService implements MatchUseCase {

    private static final Duration CHEER_CACHE_TTL = Duration.ofSeconds(5);

    private final MatchRepositoryPort matches;
    private final MatchGaugePort gauges;
    private final CatalogRepositoryPort catalog;
    private final TargetLookup lookup;
    private final FeatureGuard features;
    private final ZoneId zone;
    private final Clock clock;
    private final Map<Long, CachedMatch> cheerCache = new ConcurrentHashMap<>();

    public MatchService(MatchRepositoryPort matches, MatchGaugePort gauges, CatalogRepositoryPort catalog,
                        TargetLookup lookup, FeatureGuard features, FandomProperties properties, Clock clock) {
        this.matches = matches;
        this.gauges = gauges;
        this.catalog = catalog;
        this.lookup = lookup;
        this.features = features;
        this.zone = properties.periodKeys().zone();
        this.clock = clock;
    }

    @Override
    @Transactional
    public MatchView create(Long adminId, CreateCommand command) {
        features.matches();
        League league = catalog.findLeagueByCode(command.leagueCode()).orElseThrow(TargetErrors::leagueNotFound);
        FanTarget home = lookup.visibleTarget(command.homeTargetId());
        FanTarget away = lookup.visibleTarget(command.awayTargetId());
        if (!home.getLeagueId().equals(league.id()) || !away.getLeagueId().equals(league.id())) {
            throw MatchErrors.leagueMismatch();
        }
        Match saved = matches.save(Match.schedule(league.id(), home.getId(), away.getId(), command.startsAt(),
                command.venue(), adminId, clock.instant()));
        return view(saved);
    }

    @Override
    @Transactional
    public MatchView update(Long matchId, UpdateCommand command) {
        features.matches();
        Match match = matches.findById(matchId).orElseThrow(MatchErrors::notFound);
        Instant now = clock.instant();
        match.reschedule(command.startsAt(), now);
        if (command.homeScore() != null || command.awayScore() != null) {
            match.updateScore(command.homeScore() == null ? match.getHomeScore() : command.homeScore(),
                    command.awayScore() == null ? match.getAwayScore() : command.awayScore(), now);
        }
        match.changeStatus(command.status(), now);
        Match saved = matches.save(match);
        cheerCache.remove(matchId);
        return view(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchView> list(LocalDate date, String leagueCode) {
        features.matches();
        LocalDate day = date == null ? LocalDate.now(clock.withZone(zone)) : date;
        Long leagueId = null;
        if (leagueCode != null && !leagueCode.isBlank()) {
            leagueId = catalog.findLeagueByCode(leagueCode).map(League::id).orElseThrow(TargetErrors::leagueNotFound);
        }
        Instant from = day.atStartOfDay(zone).toInstant();
        Instant to = day.plusDays(1).atStartOfDay(zone).toInstant();
        return matches.findByStartsAtBetween(from, to, leagueId).stream().map(this::view).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchView> upcoming(int limit) {
        features.matches();
        // 진행 중인 경기가 빠지지 않도록 6시간 전 시작 경기부터 본다.
        Instant from = clock.instant().minus(Duration.ofHours(6));
        return matches.findLiveOrUpcoming(from, Math.max(1, Math.min(limit, 20))).stream().map(this::view).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MatchView get(Long matchId) {
        features.matches();
        return view(matches.findById(matchId).orElseThrow(MatchErrors::notFound));
    }

    @Override
    public MatchGauge gauge(Long matchId) {
        try {
            return gauges.read(matchId);
        } catch (RuntimeException e) {
            return MatchGauge.empty();
        }
    }

    /** 응원 하트를 받을 수 있는 경기. 하트 요청마다 DB 를 읽지 않도록 5초 캐시한다. */
    public Match cheerable(Long matchId) {
        features.matches();
        Instant now = clock.instant();
        CachedMatch cached = cheerCache.get(matchId);
        Match match;
        if (cached != null && cached.expiresAt().isAfter(now)) {
            match = cached.match();
        } else {
            match = matches.findById(matchId).orElseThrow(MatchErrors::notFound);
            cheerCache.put(matchId, new CachedMatch(match, now.plus(CHEER_CACHE_TTL)));
        }
        match.ensureAcceptsCheers();
        return match;
    }

    private MatchView view(Match match) {
        return new MatchView(match, lookup.league(match.getLeagueId()), lookup.target(match.getHomeTargetId()),
                lookup.target(match.getAwayTargetId()), gauge(match.getId()));
    }

    private record CachedMatch(Match match, Instant expiresAt) {
    }
}
