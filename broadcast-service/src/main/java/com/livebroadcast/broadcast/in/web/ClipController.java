package com.livebroadcast.broadcast.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.livebroadcast.broadcast.app.MediaProperties;
import com.livebroadcast.broadcast.app.port.in.ClipUseCase;
import com.livebroadcast.broadcast.app.port.in.ClipUseCase.CreateClip;
import com.livebroadcast.broadcast.in.web.dto.ClipResponse;
import com.livebroadcast.broadcast.in.web.dto.ClipResponse.CreateClipRequest;
import com.livebroadcast.broadcast.in.web.dto.PageResponse;
import com.livebroadcast.common.security.AuthUser;
import com.livebroadcast.common.security.LoginUser;

import jakarta.validation.Valid;

/** 하이라이트 클립. 보기는 누구나, 만들기와 지우기는 로그인 사용자 */
@RestController
public class ClipController {

    private final ClipUseCase clips;
    private final MediaProperties media;

    public ClipController(ClipUseCase clips, MediaProperties media) {
        this.clips = clips;
        this.media = media;
    }

    @GetMapping("/api/clips")
    public PageResponse<ClipResponse> list(@RequestParam(name = "target", required = false) Long targetId,
                                           @RequestParam(name = "league", required = false) String leagueCode,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        String league = leagueCode == null || leagueCode.isBlank() ? null : leagueCode.strip().toUpperCase();
        return PageResponse.from(clips.list(targetId, league, page, size).map(c -> ClipResponse.of(c, media)));
    }

    @GetMapping("/api/clips/{clipId}")
    public ClipResponse get(@PathVariable Long clipId) {
        return ClipResponse.of(clips.get(clipId), media);
    }

    @DeleteMapping("/api/clips/{clipId}")
    public ResponseEntity<Void> delete(@LoginUser AuthUser user, @PathVariable Long clipId) {
        clips.delete(clipId, user.id(), user.isAdmin());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/recordings/{recordingId}/clips")
    public List<ClipResponse> byRecording(@PathVariable Long recordingId) {
        return clips.byRecording(recordingId).stream().map(c -> ClipResponse.of(c, media)).toList();
    }

    @PostMapping("/api/recordings/{recordingId}/clips")
    public ResponseEntity<ClipResponse> create(@LoginUser AuthUser user, @PathVariable Long recordingId,
                                               @Valid @RequestBody CreateClipRequest body) {
        var clip = clips.create(new CreateClip(recordingId, user.id(), user.nickname(), body.title(),
                body.offsetSeconds(), body.durationSeconds()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ClipResponse.of(clip, media));
    }
}
