package com.livebroadcast.fandom.app.port.in;

import java.util.List;

import com.livebroadcast.fandom.domain.catalog.League;
import com.livebroadcast.fandom.domain.catalog.Sport;
import com.livebroadcast.fandom.domain.ranking.TargetStats;
import com.livebroadcast.fandom.domain.target.FanTarget;

public interface CatalogQueryUseCase {

    List<SportCatalog> sports();

    List<TargetView> targets(String leagueCode, String sort);

    TargetView target(Long targetId);

    List<TargetView> search(String keyword);

    record SportCatalog(Sport sport, List<League> leagues) {
    }

    record TargetView(FanTarget target, League league, TargetStats stats) {
    }
}
