package com.livebroadcast.fandom.app.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.livebroadcast.fandom.domain.match.MatchSide;
import com.livebroadcast.fandom.domain.prediction.Prediction;
import com.livebroadcast.fandom.domain.prediction.PredictionTally;
import com.livebroadcast.fandom.domain.prediction.PredictionRules.Outcome;

public interface PredictionRepositoryPort {

    /** 경기 시작 전이면 고를 때마다 덮어쓴다(마음 바꾸기 허용) */
    void upsert(Long matchId, Long userId, String nickname, MatchSide pick, Instant now);

    Optional<Prediction> find(Long matchId, Long userId);

    PredictionTally tally(Long matchId);

    /** 아직 정산하지 않은 예측을 한 번에 정산하고, 정산된 예측을 돌려준다(이미 정산된 건 제외) */
    List<Prediction> settle(Long matchId, Outcome outcome, Instant now);

    List<Prediction> findByUser(Long userId, int limit);

    /** 사용자의 적중 수, 정산된 전체 수 */
    int[] hitsAndTotal(Long userId);

}
