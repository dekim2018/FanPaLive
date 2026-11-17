package com.livebroadcast.broadcast.in.web.dto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import com.livebroadcast.broadcast.app.MediaProperties;
import com.livebroadcast.broadcast.domain.Clip;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 클립 응답. playbackUrl 은 MediaMTX Playback 서버의 구간 재생 주소라서 브라우저가 바로 재생한다(fMP4).
 */
public record ClipResponse(Long id, Long recordingId, Long broadcastId, Long creatorId, String creatorNickname,
                           String title, long offsetSeconds, int durationSeconds, Instant startAt, String playbackUrl,
                           BroadcastResponse.SideResponse side, Instant createdAt) {

    public static ClipResponse of(Clip c, MediaProperties media) {
        String url = media.publicPlaybackBase() + "/get"
                + "?path=" + URLEncoder.encode(c.getMediaPath(), StandardCharsets.UTF_8)
                + "&start=" + URLEncoder.encode(c.getStartAt().toString(), StandardCharsets.UTF_8)
                + "&duration=" + c.getDurationSeconds();
        return new ClipResponse(c.getId(), c.getRecordingId(), c.getBroadcastId(), c.getCreatorId(),
                c.getCreatorNickname(), c.getTitle(), c.getOffsetSeconds(), c.getDurationSeconds(), c.getStartAt(),
                url, BroadcastResponse.SideResponse.of(c.getSide()), c.getCreatedAt());
    }

    public record CreateClipRequest(@Size(max = 60) String title, @Min(0) long offsetSeconds,
                                    @Min(5) @Max(60) int durationSeconds) {
    }
}
