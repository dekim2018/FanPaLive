package com.livebroadcast.fandom.in.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.livebroadcast.fandom.app.FandomProperties;

/** 화면이 기능 플래그와 하트 규칙을 알 수 있게 공개 설정을 내려준다. */
@RestController
public class FandomConfigController {

    private final FandomProperties properties;

    public FandomConfigController(FandomProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/api/fandom/config")
    public Map<String, Object> config() {
        return Map.of(
                "features", Map.of(
                        "hearts", properties.features().hearts(),
                        "matches", properties.features().matches(),
                        "targetRequests", properties.features().targetRequests()),
                "heart", Map.of(
                        "dailyMax", properties.heartDailyMax(),
                        "perTargetMax", properties.heartPerTargetMax()),
                "primaryCooldownDays", properties.primaryCooldown().toDays(),
                "seasonStart", properties.seasonStart());
    }
}
