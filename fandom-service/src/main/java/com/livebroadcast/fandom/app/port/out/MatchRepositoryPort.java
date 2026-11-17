package com.livebroadcast.fandom.app.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.livebroadcast.fandom.domain.match.Match;

public interface MatchRepositoryPort {

    Match save(Match match);

    Optional<Match> findById(Long id);

    List<Match> findByStartsAtBetween(Instant from, Instant to, Long leagueId);

    List<Match> findLiveOrUpcoming(Instant from, int limit);
}
