package com.livebroadcast.fandom.app.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livebroadcast.fandom.app.FeatureGuard;
import com.livebroadcast.fandom.app.event.MatchClosed;
import com.livebroadcast.fandom.app.port.in.PredictionUseCase;
import com.livebroadcast.fandom.app.port.out.MatchRepositoryPort;
import com.livebroadcast.fandom.app.port.out.NotificationRepositoryPort;
import com.livebroadcast.fandom.app.port.out.PointLedgerPort;
import com.livebroadcast.fandom.app.port.out.PredictionRepositoryPort;
import com.livebroadcast.fandom.domain.level.PointReason;
import com.livebroadcast.fandom.domain.match.Match;
import com.livebroadcast.fandom.domain.match.MatchErrors;
import com.livebroadcast.fandom.domain.match.MatchSide;
import com.livebroadcast.fandom.domain.notification.NotificationType;
import com.livebroadcast.fandom.domain.prediction.Prediction;
import com.livebroadcast.fandom.domain.prediction.PredictionResult;
import com.livebroadcast.fandom.domain.prediction.PredictionRules;
import com.livebroadcast.fandom.domain.prediction.PredictionRules.Outcome;

/**
 * 승부 예측. 보상은 팬 포인트와 배지뿐이고 돈이나 후원금과 섞이지 않는다(사행성 방지).
 * 경기가 끝나면 정산을 한 번에 처리하고, 이미 정산된 예측과 포인트는 다시 처리해도 늘어나지 않는다.
 */
@Service
public class PredictionService implements PredictionUseCase {

    private final PredictionRepositoryPort predictions;
    private final MatchRepositoryPort matches;
    private final PointLedgerPort ledger;
    private final NotificationRepositoryPort notifications;
    private final FeatureGuard features;
    private final Clock clock;

    public PredictionService(PredictionRepositoryPort predictions, MatchRepositoryPort matches, PointLedgerPort ledger,
                             NotificationRepositoryPort notifications, FeatureGuard features, Clock clock) {
        this.predictions = predictions;
        this.matches = matches;
        this.ledger = ledger;
        this.notifications = notifications;
        this.features = features;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PredictionView predict(Long userId, String nickname, Long matchId, MatchSide pick) {
        features.matches();
        if (pick == null) {
            throw com.livebroadcast.common.error.DomainException.badRequest("PICK_REQUIRED", "이길 팀을 골라 주세요.");
        }
        Match match = matches.findById(matchId).orElseThrow(MatchErrors::notFound);
        PredictionRules.ensureOpen(match, clock.instant());
        predictions.upsert(matchId, userId, nickname, pick, clock.instant());
        return view(match, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public PredictionView view(Long matchId, Long userId) {
        features.matches();
        return view(matches.findById(matchId).orElseThrow(MatchErrors::notFound), userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Prediction> myPredictions(Long userId, int limit) {
        return predictions.findByUser(userId, Math.max(1, Math.min(limit, 50)));
    }

    @Override
    @Transactional
    public int settle(Long matchId) {
        Match match = matches.findById(matchId).orElseThrow(MatchErrors::notFound);
        Outcome outcome = PredictionRules.outcome(match);
        if (outcome == null) {
            return 0;
        }
        Instant now = clock.instant();
        List<Prediction> settled = predictions.settle(matchId, outcome, now);
        List<Long> hits = settled.stream().filter(p -> p.result() == PredictionResult.HIT).map(Prediction::userId)
                .toList();
        List<Long> misses = settled.stream().filter(p -> p.result() == PredictionResult.MISS)
                .map(Prediction::userId).toList();
        for (Prediction p : settled) {
            if (p.result() == PredictionResult.HIT) {
                ledger.award(p.userId(), p.nickname(), null, PointReason.PREDICTION, "match-" + matchId,
                        PointReason.PREDICTION_HIT_POINTS, now);
            }
        }
        String eventId = "prediction-" + matchId;
        String link = "/matches/" + matchId;
        if (!hits.isEmpty()) {
            notifications.insertForUsers(hits, eventId, NotificationType.PREDICTION_RESULT,
                    "승부 예측 적중!", "팬 포인트 " + PointReason.PREDICTION_HIT_POINTS + "점을 받았어요.", link, null, now);
        }
        if (!misses.isEmpty()) {
            notifications.insertForUsers(misses, eventId, NotificationType.PREDICTION_RESULT,
                    "승부 예측 결과", "아쉽게 빗나갔어요. 다음 경기에서 다시 도전해 보세요.", link, null, now);
        }
        return settled.size();
    }

    /** 경기 상태 변경과 같은 트랜잭션에서 정산한다. */
    @EventListener
    public void on(MatchClosed event) {
        settle(event.matchId());
    }

    private PredictionView view(Match match, Long userId) {
        Prediction mine = userId == null ? null : predictions.find(match.getId(), userId).orElse(null);
        boolean open = match.getStatus() == com.livebroadcast.fandom.domain.match.MatchStatus.SCHEDULED
                && clock.instant().isBefore(match.getStartsAt());
        return new PredictionView(match.getId(), predictions.tally(match.getId()), mine, open);
    }
}
