package com.livebroadcast.fandom.in.web;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.livebroadcast.common.error.DomainException;
import com.livebroadcast.common.security.AuthUser;
import com.livebroadcast.common.security.LoginUser;
import com.livebroadcast.fandom.app.port.in.FanLevelUseCase;
import com.livebroadcast.fandom.in.web.dto.ExtrasDtos.LevelResponse;
import com.livebroadcast.fandom.in.web.dto.ExtrasDtos.ProfileResponse;

@RestController
public class FanLevelController {

    private final FanLevelUseCase levels;

    public FanLevelController(FanLevelUseCase levels) {
        this.levels = levels;
    }

    /** 채팅 닉네임 옆 등급 표시용. userIds=1,2,3 (최대 100명, 비로그인 허용) */
    @GetMapping("/api/fans/levels")
    public List<LevelResponse> levels(@RequestParam String userIds) {
        List<Long> ids;
        try {
            ids = Arrays.stream(userIds.split(",")).map(String::strip).filter(s -> !s.isEmpty()).limit(100)
                    .map(Long::valueOf).distinct().toList();
        } catch (NumberFormatException e) {
            throw DomainException.badRequest("INVALID_USER_IDS", "사용자 번호 형식이 올바르지 않습니다.");
        }
        return levels.profiles(ids).values().stream().filter(Objects::nonNull).map(LevelResponse::from).toList();
    }

    @GetMapping("/api/fans/profile/me")
    public ProfileResponse me(@LoginUser AuthUser user) {
        return ProfileResponse.from(levels.profile(user.id()));
    }
}
