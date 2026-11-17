package com.livebroadcast.broadcast.domain;

import java.time.Instant;

/**
 * 하이라이트 클립. 녹화 파일을 새로 자르지 않고 "녹화의 몇 초부터 몇 초 동안"만 기록한다.
 * 재생은 MediaMTX Playback 서버의 구간 재생(/get?start=&duration=)을 쓰므로 저장 공간이 늘지 않는다.
 * 원본 녹화를 지우면 클립도 함께 지운다.
 */
public class Clip {

    public static final int MIN_SECONDS = 5;
    public static final int MAX_SECONDS = 60;
    private static final int MAX_TITLE = 60;

    private final Long id;
    private final Long recordingId;
    private final Long broadcastId;
    private final Long recordingOwnerId;
    private final String mediaPath;
    private final Long creatorId;
    private final String creatorNickname;
    private final String title;
    private final long offsetSeconds;
    private final int durationSeconds;
    private final Instant startAt;
    private final FanSide side;
    private final Instant createdAt;

    public Clip(Long id, Long recordingId, Long broadcastId, Long recordingOwnerId, String mediaPath, Long creatorId,
                String creatorNickname, String title, long offsetSeconds, int durationSeconds, Instant startAt,
                FanSide side, Instant createdAt) {
        this.id = id;
        this.recordingId = recordingId;
        this.broadcastId = broadcastId;
        this.recordingOwnerId = recordingOwnerId;
        this.mediaPath = mediaPath;
        this.creatorId = creatorId;
        this.creatorNickname = creatorNickname;
        this.title = title;
        this.offsetSeconds = offsetSeconds;
        this.durationSeconds = durationSeconds;
        this.startAt = startAt;
        this.side = side == null ? FanSide.NONE : side;
        this.createdAt = createdAt;
    }

    /**
     * @param offsetSeconds   녹화 시작부터 몇 초 뒤에서 자를지
     * @param durationSeconds 클립 길이(5~60초)
     */
    public static Clip cut(Recording recording, Long creatorId, String creatorNickname, String title,
                           long offsetSeconds, int durationSeconds, Instant now) {
        if (!recording.getStatus().isPlayable()) {
            throw BroadcastErrors.recordingNotReady();
        }
        if (durationSeconds < MIN_SECONDS || durationSeconds > MAX_SECONDS) {
            throw com.livebroadcast.common.error.DomainException.badRequest("INVALID_CLIP_LENGTH",
                    "클립 길이는 " + MIN_SECONDS + "초에서 " + MAX_SECONDS + "초 사이여야 합니다.");
        }
        long total = recording.getDurationSeconds();
        if (offsetSeconds < 0 || (total > 0 && offsetSeconds + durationSeconds > total)) {
            throw com.livebroadcast.common.error.DomainException.badRequest("INVALID_CLIP_RANGE",
                    "클립 구간이 녹화 길이를 벗어났습니다.");
        }
        String safeTitle = title == null || title.isBlank() ? recording.getTitle() + " 하이라이트" : title.strip();
        if (safeTitle.length() > MAX_TITLE) {
            safeTitle = safeTitle.substring(0, MAX_TITLE);
        }
        return new Clip(null, recording.getId(), recording.getBroadcastId(), recording.getOwnerId(),
                recording.getMediaPath(), creatorId, creatorNickname, safeTitle, offsetSeconds, durationSeconds,
                recording.getStartedAt().plusSeconds(offsetSeconds), recording.getSide(), now);
    }

    /** 만든 사람, 원본 녹화 소유자, 관리자만 지울 수 있다. */
    public void ensureDeletableBy(Long requesterId, boolean admin) {
        if (admin) {
            return;
        }
        if (requesterId == null || !(requesterId.equals(creatorId) || requesterId.equals(recordingOwnerId))) {
            throw BroadcastErrors.clipNotAllowed();
        }
    }

    public Long getId() { return id; }
    public Long getRecordingId() { return recordingId; }
    public Long getBroadcastId() { return broadcastId; }
    public Long getRecordingOwnerId() { return recordingOwnerId; }
    public String getMediaPath() { return mediaPath; }
    public Long getCreatorId() { return creatorId; }
    public String getCreatorNickname() { return creatorNickname; }
    public String getTitle() { return title; }
    public long getOffsetSeconds() { return offsetSeconds; }
    public int getDurationSeconds() { return durationSeconds; }
    public Instant getStartAt() { return startAt; }
    public FanSide getSide() { return side; }
    public Instant getCreatedAt() { return createdAt; }
}
