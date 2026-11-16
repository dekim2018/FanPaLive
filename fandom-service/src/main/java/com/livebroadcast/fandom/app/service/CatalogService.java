package com.livebroadcast.fandom.app.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livebroadcast.fandom.app.port.in.CatalogQueryUseCase;
import com.livebroadcast.fandom.app.port.out.CatalogRepositoryPort;
import com.livebroadcast.fandom.app.port.out.FanTargetRepositoryPort;
import com.livebroadcast.fandom.app.port.out.TargetStatsPort;
import com.livebroadcast.fandom.domain.catalog.League;
import com.livebroadcast.fandom.domain.ranking.TargetStats;
import com.livebroadcast.fandom.domain.target.FanTarget;
import com.livebroadcast.fandom.domain.target.TargetErrors;

@Service
public class CatalogService implements CatalogQueryUseCase {

    private final CatalogRepositoryPort catalog;
    private final FanTargetRepositoryPort targets;
    private final TargetStatsPort stats;
    private final TargetLookup lookup;

    public CatalogService(CatalogRepositoryPort catalog, FanTargetRepositoryPort targets, TargetStatsPort stats,
                          TargetLookup lookup) {
        this.catalog = catalog;
        this.targets = targets;
        this.stats = stats;
        this.lookup = lookup;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SportCatalog> sports() {
        List<League> leagues = catalog.findLeagues();
        return catalog.findSports().stream()
                .map(s -> new SportCatalog(s, leagues.stream().filter(l -> l.sportCode().equals(s.code())).toList()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TargetView> targets(String leagueCode, String sort) {
        League league = catalog.findLeagueByCode(leagueCode).orElseThrow(TargetErrors::leagueNotFound);
        List<FanTarget> list = targets.findActiveByLeague(league.id());
        Map<Long, TargetStats> statMap = stats.findStats(list.stream().map(FanTarget::getId).toList());
        Comparator<TargetView> order = "name".equalsIgnoreCase(sort)
                ? Comparator.comparing(v -> v.target().getProfile().name())
                : Comparator.<TargetView>comparingLong(v -> v.stats().totalFans()).reversed()
                        .thenComparing(v -> v.target().getProfile().name());
        return list.stream()
                .map(t -> new TargetView(t, league, statMap.getOrDefault(t.getId(), TargetStats.empty(t.getId()))))
                .sorted(order)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TargetView target(Long targetId) {
        FanTarget target = targets.findById(targetId).orElseThrow(TargetErrors::notFound);
        target.ensureVisible();
        League league = lookup.league(target.getLeagueId());
        TargetStats s = stats.findStats(List.of(targetId)).getOrDefault(targetId, TargetStats.empty(targetId));
        return new TargetView(target, league, s);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TargetView> search(String keyword) {
        if (keyword == null || keyword.strip().isEmpty()) {
            return List.of();
        }
        List<FanTarget> found = targets.search(keyword.strip(), 20);
        Map<Long, TargetStats> statMap = stats.findStats(found.stream().map(FanTarget::getId).toList());
        return found.stream()
                .map(t -> new TargetView(t, lookup.league(t.getLeagueId()),
                        statMap.getOrDefault(t.getId(), TargetStats.empty(t.getId()))))
                .toList();
    }
}
