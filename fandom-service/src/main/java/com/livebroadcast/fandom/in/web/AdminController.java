package com.livebroadcast.fandom.in.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.livebroadcast.common.error.DomainException;
import com.livebroadcast.common.security.AuthUser;
import com.livebroadcast.common.security.LoginUser;
import com.livebroadcast.fandom.app.PageResult;
import com.livebroadcast.fandom.app.port.in.CatalogQueryUseCase;
import com.livebroadcast.fandom.app.port.in.MatchUseCase;
import com.livebroadcast.fandom.app.port.in.MatchUseCase.CreateCommand;
import com.livebroadcast.fandom.app.port.in.MatchUseCase.UpdateCommand;
import com.livebroadcast.fandom.app.port.in.TargetRequestUseCase;
import com.livebroadcast.fandom.app.service.HeartAccumulator;
import com.livebroadcast.fandom.app.service.TargetLookup;
import com.livebroadcast.fandom.domain.target.RequestStatus;
import com.livebroadcast.fandom.in.web.dto.FandomDtos.CreateMatchRequest;
import com.livebroadcast.fandom.in.web.dto.FandomDtos.MatchResponse;
import com.livebroadcast.fandom.in.web.dto.FandomDtos.RejectRequest;
import com.livebroadcast.fandom.in.web.dto.FandomDtos.SimpleTarget;
import com.livebroadcast.fandom.in.web.dto.FandomDtos.TargetRequestResponse;
import com.livebroadcast.fandom.in.web.dto.FandomDtos.UpdateMatchRequest;
import com.livebroadcast.fandom.in.web.dto.FandomDtos.VisibilityRequest;
import com.livebroadcast.fandom.out.event.MatchEventHub;

import jakarta.validation.Valid;

/**
 * 관리자 기능. 보안 설정(hasRole ADMIN)으로 막고, 여기서도 역할을 한 번 더 확인한다(설정 실수 대비).
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final TargetRequestUseCase requests;
    private final MatchUseCase matches;
    private final CatalogQueryUseCase catalog;
    private final TargetLookup lookup;
    private final HeartAccumulator accumulator;
    private final MatchEventHub matchHub;

    public AdminController(TargetRequestUseCase requests, MatchUseCase matches, CatalogQueryUseCase catalog,
                           TargetLookup lookup, HeartAccumulator accumulator, MatchEventHub matchHub) {
        this.requests = requests;
        this.matches = matches;
        this.catalog = catalog;
        this.lookup = lookup;
        this.accumulator = accumulator;
        this.matchHub = matchHub;
    }

    @GetMapping("/target-requests")
    public PageResult<TargetRequestResponse> list(@LoginUser AuthUser admin,
                                                  @RequestParam(defaultValue = "PENDING") RequestStatus status,
                                                  @RequestParam(required = false) String league,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        ensureAdmin(admin);
        return requests.list(status, league, page, size)
                .map(r -> TargetRequestResponse.from(r, lookup.league(r.getLeagueId())));
    }

    @PostMapping("/target-requests/{requestId}/approve")
    public SimpleTarget approve(@LoginUser AuthUser admin, @PathVariable Long requestId) {
        ensureAdmin(admin);
        return SimpleTarget.from(requests.approve(admin.id(), requestId));
    }

    @PostMapping("/target-requests/{requestId}/reject")
    public TargetRequestResponse reject(@LoginUser AuthUser admin, @PathVariable Long requestId,
                                        @Valid @RequestBody RejectRequest body) {
        ensureAdmin(admin);
        var rejected = requests.reject(admin.id(), requestId, body.reason());
        return TargetRequestResponse.from(rejected, lookup.league(rejected.getLeagueId()));
    }

    @PatchMapping("/targets/{targetId}")
    public SimpleTarget visibility(@LoginUser AuthUser admin, @PathVariable Long targetId,
                                   @RequestBody VisibilityRequest body) {
        ensureAdmin(admin);
        return SimpleTarget.from(requests.changeVisibility(targetId, body.visible()));
    }

    @PostMapping("/matches")
    public ResponseEntity<MatchResponse> createMatch(@LoginUser AuthUser admin,
                                                     @Valid @RequestBody CreateMatchRequest body) {
        ensureAdmin(admin);
        var view = matches.create(admin.id(), new CreateCommand(body.leagueCode(), body.homeTargetId(),
                body.awayTargetId(), body.startsAt(), body.venue()));
        return ResponseEntity.status(HttpStatus.CREATED).body(MatchResponse.from(view));
    }

    @PatchMapping("/matches/{matchId}")
    public MatchResponse updateMatch(@LoginUser AuthUser admin, @PathVariable Long matchId,
                                     @RequestBody UpdateMatchRequest body) {
        ensureAdmin(admin);
        return MatchResponse.from(matches.update(matchId, new UpdateCommand(body.status(), body.homeScore(),
                body.awayScore(), body.startsAt())));
    }

    /** 운영 지표: 하트 병합 효과(쓰기 1번에 묶인 하트 수), 대기열, 실시간 구독자 수 */
    @GetMapping("/ops")
    public Map<String, Object> ops(@LoginUser AuthUser admin) {
        ensureAdmin(admin);
        HeartAccumulator.Stats s = accumulator.stats();
        double perFlush = s.flushCount() == 0 ? 0 : (double) s.flushedHearts() / s.flushCount();
        return Map.of(
                "flushedHearts", s.flushedHearts(),
                "flushCount", s.flushCount(),
                "heartsPerFlush", Math.round(perFlush * 10) / 10.0,
                "droppedHearts", s.droppedHearts(),
                "pendingHearts", s.pending(),
                "matchSubscribers", matchHub.subscriberCount(),
                "sports", catalog.sports().size());
    }

    private static void ensureAdmin(AuthUser user) {
        if (user == null || !user.isAdmin()) {
            throw DomainException.forbidden("ADMIN_ONLY", "관리자만 할 수 있는 작업입니다.");
        }
    }
}
