package com.livebroadcast.fandom.in.web.dto;

import java.time.Instant;
import java.util.List;

import com.livebroadcast.fandom.app.port.in.CatalogQueryUseCase.SportCatalog;
import com.livebroadcast.fandom.app.port.in.CatalogQueryUseCase.TargetView;
import com.livebroadcast.fandom.app.port.in.FanUseCase.MyFan;
import com.livebroadcast.fandom.app.port.in.MatchUseCase.MatchView;
import com.livebroadcast.fandom.app.port.in.RankingQueryUseCase.OverallRow;
import com.livebroadcast.fandom.app.port.in.RankingQueryUseCase.RankingBoard;
import com.livebroadcast.fandom.domain.catalog.League;
import com.livebroadcast.fandom.domain.fan.FanMembership;
import com.livebroadcast.fandom.domain.heart.HeartSource;
import com.livebroadcast.fandom.domain.match.MatchStatus;
import com.livebroadcast.fandom.domain.ranking.RankingRow;
import com.livebroadcast.fandom.domain.target.FanTarget;
import com.livebroadcast.fandom.domain.target.TargetProfile;
import com.livebroadcast.fandom.domain.target.TargetRequest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 팬덤 API 요청/응답 모음 */
public final class FandomDtos {

    private FandomDtos() {
    }

    public record LeagueResponse(Long id, String code, String name, String country, String sportCode,
                                 boolean openForRequest) {
        public static LeagueResponse from(League l) {
            return new LeagueResponse(l.id(), l.code(), l.name(), l.country(), l.sportCode(), l.openForRequest());
        }
    }

    public record SportResponse(String code, String name, String kind, List<LeagueResponse> leagues) {
        public static SportResponse from(SportCatalog c) {
            return new SportResponse(c.sport().code(), c.sport().name(), c.sport().kind().name(),
                    c.leagues().stream().map(LeagueResponse::from).toList());
        }
    }

    public record TargetResponse(Long id, String leagueCode, String leagueName, String sportCode, String kind,
                                 String name, String nameEn, String shortName, String colorPrimary,
                                 String colorSecondary, String description, boolean official, String status,
                                 long primaryFans, long followFans, long hearts, long donationTotal) {
        public static TargetResponse from(TargetView v) {
            FanTarget t = v.target();
            TargetProfile p = t.getProfile();
            return new TargetResponse(t.getId(), v.league().code(), v.league().name(), v.league().sportCode(),
                    t.getKind().name(), p.name(), p.nameEn(), p.shortName(), p.colorPrimary(), p.colorSecondary(),
                    p.description(), t.isOfficial(), t.getStatus().name(), v.stats().primaryFans(),
                    v.stats().followFans(), v.stats().hearts(), v.stats().donationTotal());
        }
    }

    /** 대상 상세. myFan 은 로그인했을 때만 채운다. */
    public record TargetDetailResponse(TargetResponse target, Integer weeklyRank, MyFanStatus myFan) {
    }

    public record MyFanStatus(boolean fan, boolean primary, Instant primaryChangeAvailableAt) {
    }

    public record SimpleTarget(Long id, String name, String shortName, String colorPrimary, String colorSecondary) {
        public static SimpleTarget from(FanTarget t) {
            TargetProfile p = t.getProfile();
            return new SimpleTarget(t.getId(), p.name(), p.shortName(), p.colorPrimary(), p.colorSecondary());
        }
    }

    public record MembershipRequest(boolean primary) {
    }

    public record MembershipResponse(Long targetId, Long leagueId, boolean primary, Instant joinedAt) {
        public static MembershipResponse from(FanMembership m) {
            return new MembershipResponse(m.targetId(), m.leagueId(), m.primary(), m.joinedAt());
        }
    }

    public record MyFanResponse(SimpleTarget target, String leagueCode, String leagueName, boolean primary,
                                Instant joinedAt, Instant primaryChangeAvailableAt) {
        public static MyFanResponse from(MyFan f) {
            return new MyFanResponse(SimpleTarget.from(f.target()), f.league().code(), f.league().name(),
                    f.membership().primary(), f.membership().joinedAt(), f.primaryChangeAvailableAt());
        }
    }

    public record HeartRequest(
            @NotNull(message = "응원할 대상을 선택해 주세요.") Long targetId,
            @Min(value = 1, message = "하트는 1개 이상 줄 수 있습니다.") @Max(value = 5, message = "한 번에 너무 많은 하트입니다.") int amount,
            HeartSource source,
            Long matchId) {
    }

    public record TargetRequestBody(
            @NotBlank(message = "리그를 선택해 주세요.") String leagueCode,
            @NotBlank(message = "이름을 입력해 주세요.") @Size(min = 2, max = 40, message = "이름은 2~40자여야 합니다.") String name,
            @Size(max = 60) String nameEn,
            @Size(max = 6, message = "약칭은 6자 이하여야 합니다.") String shortName,
            String colorPrimary,
            String colorSecondary,
            @Size(max = 300) String description,
            @Size(max = 300) String referenceUrl) {
    }

    public record TargetRequestResponse(Long id, String leagueCode, String leagueName, String kind, String name,
                                        String nameEn, String shortName, String colorPrimary, String colorSecondary,
                                        String description, String referenceUrl, String status, String rejectReason,
                                        String requesterNickname, int voteCount, Long targetId, Instant createdAt,
                                        Instant decidedAt) {
        public static TargetRequestResponse from(TargetRequest r, League league) {
            TargetProfile p = r.getProfile();
            return new TargetRequestResponse(r.getId(), league.code(), league.name(), r.getKind().name(), p.name(),
                    p.nameEn(), p.shortName(), p.colorPrimary(), p.colorSecondary(), p.description(),
                    r.getReferenceUrl(), r.getStatus().name(), r.getRejectReason(), r.getRequesterNickname(),
                    r.getVoteCount(), r.getTargetId(), r.getCreatedAt(), r.getDecidedAt());
        }
    }

    public record RequestOutcomeResponse(TargetRequestResponse request, boolean merged) {
    }

    public record RejectRequest(@NotBlank(message = "반려 사유를 입력해 주세요.") @Size(max = 200) String reason) {
    }

    public record VisibilityRequest(boolean visible) {
    }

    public record RankingResponse(LeagueResponse league, String period, String periodKey, String from, String to,
                                  List<RankingRow> rows) {
        public static RankingResponse from(RankingBoard b) {
            return new RankingResponse(LeagueResponse.from(b.league()), b.period().name(), b.periodKey(),
                    b.from().toString(), b.to().toString(), b.rows());
        }
    }

    public record OverallResponse(int rank, LeagueResponse league, RankingRow row) {
        public static OverallResponse from(OverallRow r) {
            return new OverallResponse(r.rank(), LeagueResponse.from(r.league()), r.row());
        }
    }

    public record MatchResponse(Long id, LeagueResponse league, SimpleTarget home, SimpleTarget away,
                                Instant startsAt, String status, int homeScore, int awayScore, String venue,
                                long homeHearts, long awayHearts, int homePercent) {
        public static MatchResponse from(MatchView v) {
            return new MatchResponse(v.match().getId(), LeagueResponse.from(v.league()), SimpleTarget.from(v.home()),
                    SimpleTarget.from(v.away()), v.match().getStartsAt(), v.match().getStatus().name(),
                    v.match().getHomeScore(), v.match().getAwayScore(), v.match().getVenue(), v.gauge().home(),
                    v.gauge().away(), v.gauge().homePercent());
        }
    }

    public record CreateMatchRequest(
            @NotBlank String leagueCode,
            @NotNull Long homeTargetId,
            @NotNull Long awayTargetId,
            @NotNull(message = "경기 시작 시각을 입력해 주세요.") Instant startsAt,
            @Size(max = 40) String venue) {
    }

    public record UpdateMatchRequest(MatchStatus status, Integer homeScore, Integer awayScore, Instant startsAt) {
    }
}
