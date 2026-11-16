package com.livebroadcast.fandom.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.livebroadcast.common.security.AuthUser;
import com.livebroadcast.common.security.LoginUser;
import com.livebroadcast.fandom.app.port.in.CatalogQueryUseCase;
import com.livebroadcast.fandom.app.port.in.FanUseCase;
import com.livebroadcast.fandom.app.port.in.RankingQueryUseCase;
import com.livebroadcast.fandom.domain.ranking.RankSnapshot;
import com.livebroadcast.fandom.domain.ranking.RankingPeriod;
import com.livebroadcast.fandom.domain.ranking.Supporter;
import com.livebroadcast.fandom.in.web.dto.FandomDtos.MyFanStatus;
import com.livebroadcast.fandom.in.web.dto.FandomDtos.SportResponse;
import com.livebroadcast.fandom.in.web.dto.FandomDtos.TargetDetailResponse;
import com.livebroadcast.fandom.in.web.dto.FandomDtos.TargetResponse;

/** 종목, 리그, 응원 대상 조회. 모두 비로그인 허용 */
@RestController
public class CatalogController {

    private final CatalogQueryUseCase catalog;
    private final RankingQueryUseCase ranking;
    private final FanUseCase fans;

    public CatalogController(CatalogQueryUseCase catalog, RankingQueryUseCase ranking, FanUseCase fans) {
        this.catalog = catalog;
        this.ranking = ranking;
        this.fans = fans;
    }

    @GetMapping("/api/sports")
    public List<SportResponse> sports() {
        return catalog.sports().stream().map(SportResponse::from).toList();
    }

    @GetMapping("/api/leagues/{leagueCode}/targets")
    public List<TargetResponse> targets(@PathVariable String leagueCode,
                                        @RequestParam(defaultValue = "fans") String sort) {
        return catalog.targets(leagueCode, sort).stream().map(TargetResponse::from).toList();
    }

    @GetMapping("/api/targets")
    public List<TargetResponse> search(@RequestParam(name = "q", required = false) String keyword) {
        return catalog.search(keyword).stream().map(TargetResponse::from).toList();
    }

    @GetMapping("/api/targets/{targetId}")
    public TargetDetailResponse target(@PathVariable Long targetId, @LoginUser(required = false) AuthUser user) {
        TargetResponse target = TargetResponse.from(catalog.target(targetId));
        MyFanStatus myFan = null;
        if (user != null) {
            myFan = fans.myFans(user.id()).stream()
                    .filter(f -> f.target().getId().equals(targetId))
                    .findFirst()
                    .map(f -> new MyFanStatus(true, f.membership().primary(), f.primaryChangeAvailableAt()))
                    .orElse(new MyFanStatus(false, false, null));
        }
        return new TargetDetailResponse(target, ranking.weeklyRank(targetId), myFan);
    }

    @GetMapping("/api/targets/{targetId}/supporters")
    public List<Supporter> supporters(@PathVariable Long targetId,
                                      @RequestParam(defaultValue = "SEASON") RankingPeriod period,
                                      @RequestParam(defaultValue = "10") int limit) {
        return ranking.supporters(targetId, period, limit);
    }

    @GetMapping("/api/targets/{targetId}/rank-history")
    public List<RankSnapshot> rankHistory(@PathVariable Long targetId,
                                          @RequestParam(defaultValue = "8") int limit) {
        return ranking.history(targetId, limit);
    }
}
