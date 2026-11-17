package com.livebroadcast.fandom.domain.target;

import java.time.Instant;

/**
 * 팀/선수 등록 신청. 같은 리그에 같은 이름(정규화 기준)으로 열린 신청이 있으면 새로 만들지 않고 "나도 원해요" 표를 더한다.
 * 관리자는 표가 많은 순서로 처리한다.
 */
public class TargetRequest {

    private final Long id;
    private final Long leagueId;
    private final TargetKind kind;
    private final TargetProfile profile;
    private final String referenceUrl;
    private RequestStatus status;
    private String rejectReason;
    private final Long requesterId;
    private final String requesterNickname;
    private int voteCount;
    private Long targetId;
    private Long decidedBy;
    private Instant decidedAt;
    private final Instant createdAt;

    public TargetRequest(Long id, Long leagueId, TargetKind kind, TargetProfile profile, String referenceUrl,
                         RequestStatus status, String rejectReason, Long requesterId, String requesterNickname,
                         int voteCount, Long targetId, Long decidedBy, Instant decidedAt, Instant createdAt) {
        this.id = id;
        this.leagueId = leagueId;
        this.kind = kind;
        this.profile = profile;
        this.referenceUrl = referenceUrl;
        this.status = status;
        this.rejectReason = rejectReason;
        this.requesterId = requesterId;
        this.requesterNickname = requesterNickname;
        this.voteCount = voteCount;
        this.targetId = targetId;
        this.decidedBy = decidedBy;
        this.decidedAt = decidedAt;
        this.createdAt = createdAt;
    }

    public static TargetRequest open(Long leagueId, TargetKind kind, TargetProfile profile, String referenceUrl,
                                     Long requesterId, String requesterNickname, Instant now) {
        String url = referenceUrl == null || referenceUrl.isBlank() ? null : referenceUrl.strip();
        if (url != null && (url.length() > 300 || !(url.startsWith("https://") || url.startsWith("http://")))) {
            url = null;
        }
        // 신청자 본인의 표 1개로 시작한다.
        return new TargetRequest(null, leagueId, kind, profile, url, RequestStatus.PENDING, null,
                requesterId, requesterNickname, 1, null, null, null, now);
    }

    public void addVote() {
        ensureOpen();
        this.voteCount++;
    }

    public void approve(Long adminId, Long createdTargetId, Instant now) {
        transition(RequestStatus.APPROVED);
        this.targetId = createdTargetId;
        this.decidedBy = adminId;
        this.decidedAt = now;
    }

    public void reject(Long adminId, String reason, Instant now) {
        if (reason == null || reason.isBlank()) {
            throw TargetErrors.rejectReasonRequired();
        }
        transition(RequestStatus.REJECTED);
        this.rejectReason = reason.strip().length() > 200 ? reason.strip().substring(0, 200) : reason.strip();
        this.decidedBy = adminId;
        this.decidedAt = now;
    }

    public void ensureOpen() {
        if (!status.isOpen()) {
            throw TargetErrors.requestClosed();
        }
    }

    private void transition(RequestStatus next) {
        if (!status.canTransitionTo(next)) {
            throw TargetErrors.requestClosed();
        }
        this.status = next;
    }

    public String normalizedName() {
        return profile.normalizedName();
    }

    public Long getId() { return id; }
    public Long getLeagueId() { return leagueId; }
    public TargetKind getKind() { return kind; }
    public TargetProfile getProfile() { return profile; }
    public String getReferenceUrl() { return referenceUrl; }
    public RequestStatus getStatus() { return status; }
    public String getRejectReason() { return rejectReason; }
    public Long getRequesterId() { return requesterId; }
    public String getRequesterNickname() { return requesterNickname; }
    public int getVoteCount() { return voteCount; }
    public Long getTargetId() { return targetId; }
    public Long getDecidedBy() { return decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
