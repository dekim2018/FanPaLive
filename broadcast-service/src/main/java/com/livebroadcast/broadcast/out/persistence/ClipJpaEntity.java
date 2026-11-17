package com.livebroadcast.broadcast.out.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clips")
public class ClipJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recording_id", nullable = false)
    private Long recordingId;

    @Column(name = "broadcast_id", nullable = false)
    private Long broadcastId;

    @Column(name = "recording_owner_id", nullable = false)
    private Long recordingOwnerId;

    @Column(name = "media_path", nullable = false, length = 50)
    private String mediaPath;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "creator_nickname", nullable = false, length = 20)
    private String creatorNickname;

    @Column(nullable = false, length = 60)
    private String title;

    @Column(name = "offset_seconds", nullable = false)
    private long offsetSeconds;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Embedded
    private FanSideColumns side;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ClipJpaEntity() {
    }

    ClipJpaEntity(Long id, Long recordingId, Long broadcastId, Long recordingOwnerId, String mediaPath, Long creatorId,
                  String creatorNickname, String title, long offsetSeconds, int durationSeconds, Instant startAt,
                  FanSideColumns side, Instant createdAt) {
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
        this.side = side;
        this.createdAt = createdAt;
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
    public FanSideColumns getSide() { return side; }
    public Instant getCreatedAt() { return createdAt; }
}
