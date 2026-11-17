package com.livebroadcast.broadcast.in.internal;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.livebroadcast.broadcast.app.port.in.BroadcastCommandUseCase;
import com.livebroadcast.broadcast.app.port.in.BroadcastQueryUseCase;
import com.livebroadcast.broadcast.in.internal.dto.InternalBroadcastResponse;

/**
 * 다른 서비스(interaction-service)가 방송 존재 여부와 방송자(후원 대상)를 확인할 때 사용한다.
 */
@RestController
@RequestMapping("/internal/broadcasts")
public class InternalBroadcastController {

    private final BroadcastQueryUseCase queryUseCase;
    private final BroadcastCommandUseCase commandUseCase;

    public InternalBroadcastController(BroadcastQueryUseCase queryUseCase, BroadcastCommandUseCase commandUseCase) {
        this.queryUseCase = queryUseCase;
        this.commandUseCase = commandUseCase;
    }

    @GetMapping("/{broadcastId}")
    public InternalBroadcastResponse get(@PathVariable Long broadcastId) {
        return InternalBroadcastResponse.from(queryUseCase.get(broadcastId));
    }

    /** 운영 제재(interaction-service 의 관리자 기능이 호출). 방송 중이면 종료하고 공개 목록에서 숨긴다. */
    @PostMapping("/{broadcastId}/moderate")
    public Map<String, Object> moderate(@PathVariable Long broadcastId, @RequestBody ModerateRequest body) {
        boolean ended = commandUseCase.moderate(broadcastId, body == null ? null : body.reason());
        return Map.of("broadcastId", broadcastId, "ended", ended);
    }

    public record ModerateRequest(String reason) {
    }
}
