package com.livebroadcast.fandom.app.port.in;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.livebroadcast.fandom.domain.catalog.League;
import com.livebroadcast.fandom.domain.match.Match;
import com.livebroadcast.fandom.domain.match.MatchGauge;
import com.livebroadcast.fandom.domain.match.MatchStatus;
import com.livebroadcast.fandom.domain.target.FanTarget;

public interface MatchUseCase {

    MatchView create(Long adminId, CreateCommand command);

    MatchView update(Long matchId, UpdateCommand command);

    List<MatchView> list(LocalDate date, String leagueCode);

    List<MatchView> upcoming(int limit);

    MatchView get(Long matchId);

    MatchGauge gauge(Long matchId);

    record CreateCommand(String leagueCode, Long homeTargetId, Long awayTargetId, Instant startsAt, String venue) {
    }

    record UpdateCommand(MatchStatus status, Integer homeScore, Integer awayScore, Instant startsAt) {
    }

    record MatchView(Match match, League league, FanTarget home, FanTarget away, MatchGauge gauge) {
    }
}
