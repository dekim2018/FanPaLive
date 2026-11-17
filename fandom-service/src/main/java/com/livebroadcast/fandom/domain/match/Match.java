package com.livebroadcast.fandom.domain.match;

import java.time.Instant;
import java.util.Objects;

/**
 * 관리자가 등록하는 경기. 매치룸은 양 팀의 편파방과 응원 하트 게이지를 한 화면에 보여준다.
 */
public class Match {

    private final Long id;
    private final Long leagueId;
    private final Long homeTargetId;
    private final Long awayTargetId;
    private Instant startsAt;
    private MatchStatus status;
    private int homeScore;
    private int awayScore;
    private String venue;
    private final Long createdBy;
    private final Instant createdAt;
    private Instant updatedAt;

    public Match(Long id, Long leagueId, Long homeTargetId, Long awayTargetId, Instant startsAt, MatchStatus status,
                 int homeScore, int awayScore, String venue, Long createdBy, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.leagueId = leagueId;
        this.homeTargetId = homeTargetId;
        this.awayTargetId = awayTargetId;
        this.startsAt = startsAt;
        this.status = status;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.venue = venue;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Match schedule(Long leagueId, Long homeTargetId, Long awayTargetId, Instant startsAt, String venue,
                                 Long adminId, Instant now) {
        if (Objects.equals(homeTargetId, awayTargetId)) {
            throw MatchErrors.sameTeams();
        }
        Objects.requireNonNull(startsAt, "startsAt");
        String safeVenue = venue == null ? "" : venue.strip();
        if (safeVenue.length() > 40) {
            safeVenue = safeVenue.substring(0, 40);
        }
        return new Match(null, leagueId, homeTargetId, awayTargetId, startsAt, MatchStatus.SCHEDULED, 0, 0,
                safeVenue, adminId, now, now);
    }

    public void changeStatus(MatchStatus next, Instant now) {
        if (next == null || next == status) {
            return;
        }
        if (!status.canTransitionTo(next)) {
            throw MatchErrors.invalidTransition(status, next);
        }
        this.status = next;
        this.updatedAt = now;
    }

    public void updateScore(int home, int away, Instant now) {
        if (home < 0 || away < 0) {
            throw MatchErrors.invalidScore();
        }
        this.homeScore = home;
        this.awayScore = away;
        this.updatedAt = now;
    }

    public void reschedule(Instant startsAt, Instant now) {
        if (startsAt != null && status == MatchStatus.SCHEDULED) {
            this.startsAt = startsAt;
            this.updatedAt = now;
        }
    }

    public boolean involves(Long targetId) {
        return Objects.equals(homeTargetId, targetId) || Objects.equals(awayTargetId, targetId);
    }

    public MatchSide sideOf(Long targetId) {
        if (Objects.equals(homeTargetId, targetId)) return MatchSide.HOME;
        if (Objects.equals(awayTargetId, targetId)) return MatchSide.AWAY;
        throw com.livebroadcast.fandom.domain.heart.HeartErrors.notInMatch();
    }

    public void ensureAcceptsCheers() {
        if (!status.acceptsCheers()) {
            throw MatchErrors.closed();
        }
    }

    public Long getId() { return id; }
    public Long getLeagueId() { return leagueId; }
    public Long getHomeTargetId() { return homeTargetId; }
    public Long getAwayTargetId() { return awayTargetId; }
    public Instant getStartsAt() { return startsAt; }
    public MatchStatus getStatus() { return status; }
    public int getHomeScore() { return homeScore; }
    public int getAwayScore() { return awayScore; }
    public String getVenue() { return venue; }
    public Long getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
