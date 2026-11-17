package com.livebroadcast.broadcast.app.port.in;

import java.util.List;

import com.livebroadcast.broadcast.app.PageResult;
import com.livebroadcast.broadcast.domain.Recording;

public interface RecordingUseCase {

    PageResult<Recording> list(Long broadcastId, int page, int size);

    /** 다시보기 필터(방송, 응원팀, 리그). null 인 조건은 쓰지 않는다. */
    PageResult<Recording> list(RecordingFilter filter, int page, int size);

    record RecordingFilter(Long broadcastId, Long targetId, String leagueCode) {
    }

    RecordingDetail get(Long recordingId);

    void delete(Long recordingId, Long requesterId);

    record RecordingDetail(Recording recording, List<PlaybackSegment> segments) {
    }

    record PlaybackSegment(String url, String start, double durationSeconds) {
    }
}
