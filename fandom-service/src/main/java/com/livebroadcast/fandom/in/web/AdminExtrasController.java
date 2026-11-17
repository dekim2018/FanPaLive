package com.livebroadcast.fandom.in.web;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.livebroadcast.common.error.DomainException;
import com.livebroadcast.common.security.AuthUser;
import com.livebroadcast.common.security.LoginUser;
import com.livebroadcast.fandom.app.FandomProperties;
import com.livebroadcast.fandom.app.port.in.HallOfFameUseCase;
import com.livebroadcast.fandom.app.port.in.MatchImportUseCase;
import com.livebroadcast.fandom.app.port.in.MatchImportUseCase.ImportResult;
import com.livebroadcast.fandom.in.web.dto.ExtrasDtos.ImportRequest;

import jakarta.validation.Valid;

/** 관리자: 경기 일정 가져오기, 시즌 마감 수동 실행 */
@RestController
@RequestMapping("/api/admin")
public class AdminExtrasController {

    private final MatchImportUseCase imports;
    private final HallOfFameUseCase hallOfFame;
    private final FandomProperties properties;
    private final Clock clock;

    public AdminExtrasController(MatchImportUseCase imports, HallOfFameUseCase hallOfFame,
                                 FandomProperties properties, Clock clock) {
        this.imports = imports;
        this.hallOfFame = hallOfFame;
        this.properties = properties;
        this.clock = clock;
    }

    /** 저장하지 않고 각 줄이 새 경기/중복/오류 중 무엇인지 보여준다 */
    @PostMapping("/matches/import/preview")
    public ImportResult preview(@LoginUser AuthUser admin, @Valid @RequestBody ImportRequest body) {
        ensureAdmin(admin);
        return imports.preview(body.leagueCode(), body.payload());
    }

    @PostMapping("/matches/import")
    public ImportResult run(@LoginUser AuthUser admin, @Valid @RequestBody ImportRequest body) {
        ensureAdmin(admin);
        return imports.run(admin.id(), body.leagueCode(), body.payload());
    }

    /** date 가 속한 시즌을 지금 기준으로 마감한다(없으면 오늘). 다시 실행하면 최신 값으로 덮어쓴다. */
    @PostMapping("/seasons/close")
    public Map<String, Integer> closeSeason(@LoginUser AuthUser admin,
                                            @RequestParam(required = false)
                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        ensureAdmin(admin);
        LocalDate target = date != null ? date : LocalDate.now(clock.withZone(properties.periodKeys().zone()));
        return Map.of("leagues", hallOfFame.closeSeason(target));
    }

    private static void ensureAdmin(AuthUser user) {
        if (user == null || !user.isAdmin()) {
            throw DomainException.forbidden("ADMIN_ONLY", "관리자만 할 수 있는 작업입니다.");
        }
    }
}
