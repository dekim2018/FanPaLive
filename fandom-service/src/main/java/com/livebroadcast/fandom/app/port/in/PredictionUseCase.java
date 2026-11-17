package com.livebroadcast.fandom.app.port.in;

import java.util.List;

import com.livebroadcast.fandom.domain.match.MatchSide;
import com.livebroadcast.fandom.domain.prediction.Prediction;
import com.livebroadcast.fandom.domain.prediction.PredictionTally;

public interface PredictionUseCase {

    PredictionView predict(Long userId, String nickname, Long matchId, MatchSide pick);

    /** userId 가 null 이면 분포만 */
    PredictionView view(Long matchId, Long userId);

    List<Prediction> myPredictions(Long userId, int limit);

    /** 경기 종료/취소 때 정산. 정산한 예측 수 */
    int settle(Long matchId);

    record PredictionView(Long matchId, PredictionTally tally, Prediction mine, boolean open) {
    }
}
