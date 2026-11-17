package com.livebroadcast.fandom.app.port.out;

import java.util.List;
import java.util.Optional;

import com.livebroadcast.fandom.domain.catalog.League;
import com.livebroadcast.fandom.domain.catalog.Sport;

public interface CatalogRepositoryPort {

    List<Sport> findSports();

    List<League> findLeagues();

    Optional<League> findLeagueByCode(String code);

    Optional<League> findLeagueById(Long id);
}
