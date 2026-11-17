package com.livebroadcast.fandom.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.livebroadcast.common.security.AuthUser;
import com.livebroadcast.common.security.LoginUser;
import com.livebroadcast.fandom.app.port.in.PredictionUseCase;
import com.livebroadcast.fandom.in.web.dto.ExtrasDtos.MyPredictionResponse;
import com.livebroadcast.fandom.in.web.dto.ExtrasDtos.PredictRequest;
import com.livebroadcast.fandom.in.web.dto.ExtrasDtos.PredictionResponse;

import jakarta.validation.Valid;

/** 승부 예측. 경기 시작 전까지만 고를 수 있고 바꿀 수도 있다. 보상은 팬 포인트와 배지뿐이다. */
@RestController
public class PredictionController {

    private final PredictionUseCase predictions;

    public PredictionController(PredictionUseCase predictions) {
        this.predictions = predictions;
    }

    /** 분포는 누구나, 내 선택은 로그인했을 때만 */
    @GetMapping("/api/matches/{matchId}/prediction")
    public PredictionResponse view(@LoginUser(required = false) AuthUser user, @PathVariable Long matchId) {
        return PredictionResponse.from(predictions.view(matchId, user == null ? null : user.id()));
    }

    @PostMapping("/api/matches/{matchId}/prediction")
    public PredictionResponse predict(@LoginUser AuthUser user, @PathVariable Long matchId,
                                      @Valid @RequestBody PredictRequest body) {
        return PredictionResponse.from(predictions.predict(user.id(), user.nickname(), matchId, body.pick()));
    }

    @GetMapping("/api/predictions/me")
    public List<MyPredictionResponse> mine(@LoginUser AuthUser user,
                                           @RequestParam(defaultValue = "20") int limit) {
        return predictions.myPredictions(user.id(), limit).stream().map(MyPredictionResponse::from).toList();
    }
}
