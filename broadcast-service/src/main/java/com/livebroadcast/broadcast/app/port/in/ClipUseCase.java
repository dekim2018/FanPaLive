package com.livebroadcast.broadcast.app.port.in;

import java.util.List;

import com.livebroadcast.broadcast.app.PageResult;
import com.livebroadcast.broadcast.domain.Clip;

public interface ClipUseCase {

    Clip create(CreateClip command);

    PageResult<Clip> list(Long targetId, String leagueCode, int page, int size);

    List<Clip> byRecording(Long recordingId);

    Clip get(Long clipId);

    void delete(Long clipId, Long requesterId, boolean admin);

    record CreateClip(Long recordingId, Long creatorId, String creatorNickname, String title, long offsetSeconds,
                      int durationSeconds) {
    }
}
