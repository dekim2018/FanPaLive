package com.livebroadcast.fandom.in.web;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.livebroadcast.common.security.AuthUser;
import com.livebroadcast.common.security.LoginUser;
import com.livebroadcast.fandom.app.port.in.FanUseCase;
import com.livebroadcast.fandom.app.port.in.FanUseCase.Badge;
import com.livebroadcast.fandom.in.web.dto.FandomDtos.MembershipRequest;
import com.livebroadcast.fandom.in.web.dto.FandomDtos.MembershipResponse;
import com.livebroadcast.fandom.in.web.dto.FandomDtos.MyFanResponse;

@RestController
public class FanController {

    private final FanUseCase fans;

    public FanController(FanUseCase fans) {
        this.fans = fans;
    }

    /** 팬 등록. primary=true 면 그 리그의 주 응원팀으로 정한다. */
    @PutMapping("/api/fans/{targetId}")
    public MembershipResponse join(@LoginUser AuthUser user, @PathVariable Long targetId,
                                   @RequestBody(required = false) MembershipRequest request) {
        boolean primary = request != null && request.primary();
        return MembershipResponse.from(fans.join(user.id(), user.nickname(), targetId, primary));
    }

    @DeleteMapping("/api/fans/{targetId}")
    public ResponseEntity<Void> leave(@LoginUser AuthUser user, @PathVariable Long targetId) {
        fans.leave(user.id(), targetId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/fans/me")
    public List<MyFanResponse> mine(@LoginUser AuthUser user) {
        return fans.myFans(user.id()).stream().map(MyFanResponse::from).toList();
    }

    /** 채팅, 후원 알림에 붙일 주 응원팀 배지. userIds=1,2,3 (최대 100명) */
    @GetMapping("/api/fans/badges")
    public Map<String, List<Badge>> badges(@RequestParam String userIds) {
        List<Long> ids = Arrays.stream(userIds.split(","))
                .map(String::strip)
                .filter(s -> s.matches("\\d{1,18}"))
                .map(Long::valueOf)
                .filter(Objects::nonNull)
                .limit(100)
                .toList();
        Map<String, List<Badge>> result = new LinkedHashMap<>();
        fans.badges(ids).forEach((id, list) -> result.put(String.valueOf(id), list));
        return result;
    }
}
