package com.livebroadcast.fandom.in.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.livebroadcast.fandom.app.port.in.MatchUseCase;
import com.livebroadcast.fandom.in.web.dto.FandomDtos.MatchResponse;
import com.livebroadcast.fandom.out.event.MatchEventHub;

@RestController
public class MatchController {

    private final MatchUseCase matches;
    private final MatchEventHub hub;

    public MatchController(MatchUseCase matches, MatchEventHub hub) {
        this.matches = matches;
        this.hub = hub;
    }

    @GetMapping("/api/matches")
    public List<MatchResponse> list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                    LocalDate date,
                                    @RequestParam(required = false) String league) {
        return matches.list(date, league).stream().map(MatchResponse::from).toList();
    }

    @GetMapping("/api/matches/upcoming")
    public List<MatchResponse> upcoming(@RequestParam(defaultValue = "6") int limit) {
        return matches.upcoming(limit).stream().map(MatchResponse::from).toList();
    }

    @GetMapping("/api/matches/{matchId}")
    public MatchResponse get(@PathVariable Long matchId) {
        return MatchResponse.from(matches.get(matchId));
    }

    /** 매치룸 응원 게이지 실시간 구독(비로그인 허용) */
    @GetMapping(value = "/api/matches/{matchId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable Long matchId) {
        matches.get(matchId);
        return hub.subscribe(matchId, matches.gauge(matchId));
    }
}
