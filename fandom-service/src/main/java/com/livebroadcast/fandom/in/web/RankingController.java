package com.livebroadcast.fandom.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.livebroadcast.fandom.app.port.in.RankingQueryUseCase;
import com.livebroadcast.fandom.domain.ranking.RankingPeriod;
import com.livebroadcast.fandom.domain.ranking.RankingSort;
import com.livebroadcast.fandom.in.web.dto.FandomDtos.OverallResponse;
import com.livebroadcast.fandom.in.web.dto.FandomDtos.RankingResponse;

@RestController
public class RankingController {

    private final RankingQueryUseCase ranking;

    public RankingController(RankingQueryUseCase ranking) {
        this.ranking = ranking;
    }

    /** 리그별 순위. period=WEEKLY|MONTHLY|SEASON, sort=SCORE|FANS|HEARTS|DONATIONS */
    @GetMapping("/api/rankings")
    public RankingResponse ranking(@RequestParam(defaultValue = "KBO") String league,
                                   @RequestParam(defaultValue = "WEEKLY") RankingPeriod period,
                                   @RequestParam(defaultValue = "SCORE") RankingSort sort) {
        return RankingResponse.from(ranking.ranking(league, period, sort));
    }

    /** 종목 통합 TOP (팬 수 기준) */
    @GetMapping("/api/rankings/overall")
    public List<OverallResponse> overall(@RequestParam(defaultValue = "10") int limit) {
        return ranking.overall(limit).stream().map(OverallResponse::from).toList();
    }
}
