package com.livebroadcast.fandom.in.internal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.livebroadcast.fandom.app.port.in.MatchUseCase;
import com.livebroadcast.fandom.app.service.TargetLookup;
import com.livebroadcast.fandom.domain.catalog.League;
import com.livebroadcast.fandom.domain.match.Match;
import com.livebroadcast.fandom.domain.target.FanTarget;

/**
 * 다른 서비스가 응원 대상과 경기를 확인할 때 쓰는 내부 API.
 * broadcast-service(편파방 응원팀 지정), interaction-service(구단 응원기금 후원 대상 확인)가 호출한다.
 */
@RestController
@RequestMapping("/internal")
public class InternalFandomController {

    private final TargetLookup lookup;
    private final MatchUseCase matches;

    public InternalFandomController(TargetLookup lookup, MatchUseCase matches) {
        this.lookup = lookup;
        this.matches = matches;
    }

    @GetMapping("/targets/{targetId}")
    public InternalTarget target(@PathVariable Long targetId) {
        FanTarget t = lookup.target(targetId);
        League league = lookup.league(t.getLeagueId());
        return new InternalTarget(t.getId(), league.code(), t.getProfile().name(), t.getProfile().shortName(),
                t.getProfile().colorPrimary(), t.getProfile().colorSecondary(), t.getStatus().name());
    }

    @GetMapping("/matches/{matchId}")
    public InternalMatch match(@PathVariable Long matchId) {
        Match m = matches.get(matchId).match();
        return new InternalMatch(m.getId(), lookup.league(m.getLeagueId()).code(), m.getHomeTargetId(),
                m.getAwayTargetId(), m.getStatus().name());
    }

    public record InternalTarget(Long id, String leagueCode, String name, String shortName, String colorPrimary,
                                 String colorSecondary, String status) {
    }

    public record InternalMatch(Long id, String leagueCode, Long homeTargetId, Long awayTargetId, String status) {
    }
}
