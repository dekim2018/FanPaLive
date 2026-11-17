package com.livebroadcast.fandom.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.livebroadcast.fandom.app.port.in.HallOfFameUseCase;
import com.livebroadcast.fandom.in.web.dto.ExtrasDtos.SeasonResponse;

@RestController
public class HallOfFameController {

    private final HallOfFameUseCase hallOfFame;

    public HallOfFameController(HallOfFameUseCase hallOfFame) {
        this.hallOfFame = hallOfFame;
    }

    /** 지난 시즌들의 최종 순위(상위 10)와 우승팀 MVP 팬. 최신 시즌부터 */
    @GetMapping("/api/hall-of-fame")
    public List<SeasonResponse> seasons(@RequestParam(defaultValue = "KBO") String league) {
        return hallOfFame.seasons(league).stream().map(SeasonResponse::from).toList();
    }
}
