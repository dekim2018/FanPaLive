package com.livebroadcast.fandom.domain.prediction;

import java.time.Instant;

import com.livebroadcast.fandom.domain.match.MatchSide;

/** 경기 승부 예측 한 건(사용자당 경기마다 하나). 보상은 팬 포인트뿐이고 돈과 섞이지 않는다. */
public record Prediction(Long matchId, Long userId, String nickname, MatchSide pick, Instant createdAt,
                         PredictionResult result, Instant settledAt) {

    public boolean isSettled() {
        return result != null;
    }
}
