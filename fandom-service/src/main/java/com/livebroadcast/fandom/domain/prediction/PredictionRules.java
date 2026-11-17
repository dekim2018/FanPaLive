package com.livebroadcast.fandom.domain.prediction;

import java.time.Instant;

import com.livebroadcast.common.error.DomainException;
import com.livebroadcast.fandom.domain.match.Match;
import com.livebroadcast.fandom.domain.match.MatchSide;
import com.livebroadcast.fandom.domain.match.MatchStatus;

/** 승부 예측 규칙: 경기 시작 전까지만 받고, 경기가 끝나면 한 번에 정산한다. */
public final class PredictionRules {

    private PredictionRules() {
    }

    public static void ensureOpen(Match match, Instant now) {
        if (match.getStatus() != MatchStatus.SCHEDULED || !now.isBefore(match.getStartsAt())) {
            throw DomainException.conflict("PREDICTION_CLOSED", "경기가 시작되어 더 이상 예측할 수 없습니다.");
        }
    }

    /** 정산 결과. FINISHED 가 아니면 정산하지 않는다(null). 무승부와 취소는 모두 VOID */
    public static Outcome outcome(Match match) {
        if (match.getStatus() == MatchStatus.CANCELED) {
            return new Outcome(null);
        }
        if (match.getStatus() != MatchStatus.FINISHED) {
            return null;
        }
        if (match.getHomeScore() == match.getAwayScore()) {
            return new Outcome(null);
        }
        return new Outcome(match.getHomeScore() > match.getAwayScore() ? MatchSide.HOME : MatchSide.AWAY);
    }

    /** @param winner 이긴 쪽. 무승부, 취소면 null */
    public record Outcome(MatchSide winner) {
        public PredictionResult resultOf(MatchSide pick) {
            if (winner == null) {
                return PredictionResult.VOID;
            }
            return winner == pick ? PredictionResult.HIT : PredictionResult.MISS;
        }
    }
}
