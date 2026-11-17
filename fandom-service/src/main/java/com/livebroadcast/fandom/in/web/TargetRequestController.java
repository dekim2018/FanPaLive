package com.livebroadcast.fandom.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.livebroadcast.common.security.AuthUser;
import com.livebroadcast.common.security.LoginUser;
import com.livebroadcast.fandom.app.PageResult;
import com.livebroadcast.fandom.app.port.in.TargetRequestUseCase;
import com.livebroadcast.fandom.app.port.in.TargetRequestUseCase.RequestCommand;
import com.livebroadcast.fandom.app.service.TargetLookup;
import com.livebroadcast.fandom.domain.target.RequestStatus;
import com.livebroadcast.fandom.domain.target.TargetRequest;
import com.livebroadcast.fandom.in.web.dto.FandomDtos.RequestOutcomeResponse;
import com.livebroadcast.fandom.in.web.dto.FandomDtos.TargetRequestBody;
import com.livebroadcast.fandom.in.web.dto.FandomDtos.TargetRequestResponse;

import jakarta.validation.Valid;

/** 해외 팀/선수 등록 신청과 "나도 원해요" 투표 */
@RestController
public class TargetRequestController {

    private final TargetRequestUseCase requests;
    private final TargetLookup lookup;

    public TargetRequestController(TargetRequestUseCase requests, TargetLookup lookup) {
        this.requests = requests;
        this.lookup = lookup;
    }

    @PostMapping("/api/target-requests")
    public ResponseEntity<RequestOutcomeResponse> request(@LoginUser AuthUser user,
                                                          @Valid @RequestBody TargetRequestBody body) {
        var outcome = requests.request(new RequestCommand(user.id(), user.nickname(), body.leagueCode(), body.name(),
                body.nameEn(), body.shortName(), body.colorPrimary(), body.colorSecondary(), body.description(),
                body.referenceUrl()));
        return ResponseEntity.status(outcome.merged() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(new RequestOutcomeResponse(toResponse(outcome.request()), outcome.merged()));
    }

    @PostMapping("/api/target-requests/{requestId}/votes")
    public TargetRequestResponse vote(@LoginUser AuthUser user, @PathVariable Long requestId) {
        return toResponse(requests.vote(requestId, user.id(), user.nickname()));
    }

    /** 투표를 모으는 중인 신청 목록(비로그인 허용) */
    @GetMapping("/api/target-requests")
    public PageResult<TargetRequestResponse> open(@RequestParam(required = false) String league,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return requests.list(RequestStatus.PENDING, league, page, size).map(this::toResponse);
    }

    /** 내가 신청했거나 투표한 신청 */
    @GetMapping("/api/target-requests/mine")
    public List<TargetRequestResponse> mine(@LoginUser AuthUser user) {
        return requests.mine(user.id()).stream().map(this::toResponse).toList();
    }

    TargetRequestResponse toResponse(TargetRequest r) {
        return TargetRequestResponse.from(r, lookup.league(r.getLeagueId()));
    }
}
