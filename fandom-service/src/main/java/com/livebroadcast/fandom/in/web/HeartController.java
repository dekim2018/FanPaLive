package com.livebroadcast.fandom.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.livebroadcast.common.security.AuthUser;
import com.livebroadcast.common.security.LoginUser;
import com.livebroadcast.fandom.app.port.in.HeartUseCase;
import com.livebroadcast.fandom.app.port.in.HeartUseCase.GiveCommand;
import com.livebroadcast.fandom.app.port.in.HeartUseCase.Remaining;
import com.livebroadcast.fandom.domain.heart.HeartResult;
import com.livebroadcast.fandom.in.web.dto.FandomDtos.HeartRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
public class HeartController {

    private final HeartUseCase hearts;

    public HeartController(HeartUseCase hearts) {
        this.hearts = hearts;
    }

    @PostMapping("/api/hearts")
    public HeartResult give(@LoginUser AuthUser user, @Valid @RequestBody HeartRequest body,
                            HttpServletRequest request) {
        return hearts.give(new GiveCommand(user.id(), user.nickname(), user.joinedAt(), body.targetId(),
                body.amount(), body.source(), body.matchId(), RequestContext.deviceId(request),
                RequestContext.clientIp(request)));
    }

    @GetMapping("/api/hearts/me")
    public Remaining remaining(@LoginUser AuthUser user) {
        return hearts.remaining(user.id());
    }
}
