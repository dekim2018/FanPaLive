package com.livebroadcast.fandom.domain.target;

import java.time.Instant;

/**
 * 팬이 될 수 있는 대상(구단 또는 선수). 공식 대상(KBO 10구단)은 운영자가 넣고,
 * 나머지는 사용자가 신청하고 관리자가 승인하면 만들어진다.
 */
public class FanTarget {

    private final Long id;
    private final Long leagueId;
    private final TargetKind kind;
    private TargetProfile profile;
    private TargetStatus status;
    private final boolean official;
    private final Long createdBy;
    private final Instant createdAt;

    public FanTarget(Long id, Long leagueId, TargetKind kind, TargetProfile profile, TargetStatus status,
                     boolean official, Long createdBy, Instant createdAt) {
        this.id = id;
        this.leagueId = leagueId;
        this.kind = kind;
        this.profile = profile;
        this.status = status;
        this.official = official;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    /** 승인된 신청서로 대상을 만든다. */
    public static FanTarget fromRequest(TargetRequest request, Instant now) {
        return new FanTarget(null, request.getLeagueId(), request.getKind(), request.getProfile(),
                TargetStatus.ACTIVE, false, request.getRequesterId(), now);
    }

    public void ensureVisible() {
        if (!status.isVisible()) {
            throw TargetErrors.notVisible();
        }
    }

    public void hide() {
        this.status = TargetStatus.HIDDEN;
    }

    public void show() {
        this.status = TargetStatus.ACTIVE;
    }

    public void updateProfile(TargetProfile profile) {
        this.profile = profile;
    }

    public Long getId() { return id; }
    public Long getLeagueId() { return leagueId; }
    public TargetKind getKind() { return kind; }
    public TargetProfile getProfile() { return profile; }
    public TargetStatus getStatus() { return status; }
    public boolean isOfficial() { return official; }
    public Long getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
