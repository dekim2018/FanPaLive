package com.livebroadcast.fandom.in.web.dto;

import java.time.Instant;
import java.util.List;

import com.livebroadcast.fandom.app.port.in.PredictionUseCase.PredictionView;
import com.livebroadcast.fandom.domain.level.FanProfile;
import com.livebroadcast.fandom.domain.match.MatchSide;
import com.livebroadcast.fandom.domain.notification.Notification;
import com.livebroadcast.fandom.domain.prediction.Prediction;
import com.livebroadcast.fandom.domain.ranking.SeasonRecord;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 알림, 팬 등급, 승부 예측, 명예의 전당, 일정 가져오기 요청/응답 */
public final class ExtrasDtos {

    private ExtrasDtos() {
    }

    public record NotificationResponse(Long id, String type, String title, String body, String link, Long targetId,
                                       Instant createdAt, boolean read) {
        public static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.id(), n.type().name(), n.title(), n.body(), n.link(), n.targetId(),
                    n.createdAt(), n.readAt() != null);
        }
    }

    public record NotificationListResponse(List<NotificationResponse> content, int page, int size,
                                           long totalElements, int totalPages, long unreadCount) {
    }

    /** ids 를 비우면 전부 읽음 */
    public record MarkReadRequest(@Size(max = 200) List<Long> ids) {
    }

    public record TicketResponse(String ticket, int expiresInSeconds) {
    }

    public record AchievementResponse(String code, String label, String description) {
    }

    public record LevelResponse(Long userId, String level, String levelLabel, long points) {
        public static LevelResponse from(FanProfile p) {
            return new LevelResponse(p.userId(), p.level().name(), p.level().label(), p.points());
        }
    }

    public record ProfileResponse(Long userId, String level, String levelLabel, long points, String nextLevel,
                                  String nextLevelLabel, long pointsToNext, int predictionHits, int predictionTotal,
                                  int heartDays, int donationCount, List<AchievementResponse> achievements) {
        public static ProfileResponse from(FanProfile p) {
            var next = p.level().next();
            return new ProfileResponse(p.userId(), p.level().name(), p.level().label(), p.points(),
                    next == null ? null : next.name(), next == null ? null : next.label(), p.pointsToNext(),
                    p.predictionHits(), p.predictionTotal(), p.heartDays(), p.donationCount(),
                    p.achievements().stream()
                            .map(a -> new AchievementResponse(a.name(), a.label(), a.description())).toList());
        }
    }

    public record PredictRequest(@NotNull MatchSide pick) {
    }

    public record MyPick(String pick, String result) {
        static MyPick from(Prediction p) {
            return p == null ? null : new MyPick(p.pick().name(), p.result() == null ? null : p.result().name());
        }
    }

    public record PredictionResponse(Long matchId, long home, long away, int homePercent, boolean open, MyPick mine) {
        public static PredictionResponse from(PredictionView v) {
            return new PredictionResponse(v.matchId(), v.tally().home(), v.tally().away(), v.tally().homePercent(),
                    v.open(), MyPick.from(v.mine()));
        }
    }

    public record MyPredictionResponse(Long matchId, String pick, String result, Instant createdAt,
                                       Instant settledAt) {
        public static MyPredictionResponse from(Prediction p) {
            return new MyPredictionResponse(p.matchId(), p.pick().name(),
                    p.result() == null ? null : p.result().name(), p.createdAt(), p.settledAt());
        }
    }

    public record SeasonMvp(Long userId, String nickname, long points) {
    }

    public record SeasonResponse(String seasonKey, List<SeasonRecord.SeasonRow> rows, SeasonMvp mvp) {
        public static SeasonResponse from(SeasonRecord r) {
            return new SeasonResponse(r.seasonKey(), r.rows(), r.mvp() == null ? null
                    : new SeasonMvp(r.mvp().userId(), r.mvp().nickname(), r.mvp().points()));
        }
    }

    public record ImportRequest(@NotBlank String leagueCode, @NotBlank @Size(max = 100_000) String payload) {
    }
}
