package com.livebroadcast.broadcast.app.service;

import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livebroadcast.broadcast.app.PageResult;
import com.livebroadcast.broadcast.app.port.in.ClipUseCase;
import com.livebroadcast.broadcast.app.port.out.ClipRepositoryPort;
import com.livebroadcast.broadcast.app.port.out.RecordingRepositoryPort;
import com.livebroadcast.broadcast.domain.BroadcastErrors;
import com.livebroadcast.broadcast.domain.Clip;
import com.livebroadcast.broadcast.domain.Recording;
import com.livebroadcast.common.error.DomainException;

@Service
public class ClipService implements ClipUseCase {

    /** 한 사람이 녹화 하나에서 만들 수 있는 클립 수(도배 방지) */
    static final int MAX_PER_RECORDING = 20;

    private final ClipRepositoryPort clips;
    private final RecordingRepositoryPort recordings;
    private final Clock clock;

    public ClipService(ClipRepositoryPort clips, RecordingRepositoryPort recordings, Clock clock) {
        this.clips = clips;
        this.recordings = recordings;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Clip create(CreateClip c) {
        Recording recording = recordings.findById(c.recordingId()).orElseThrow(BroadcastErrors::recordingNotFound);
        if (clips.countByRecordingAndCreator(c.recordingId(), c.creatorId()) >= MAX_PER_RECORDING) {
            throw DomainException.conflict("TOO_MANY_CLIPS",
                    "녹화 하나에서 클립은 " + MAX_PER_RECORDING + "개까지 만들 수 있습니다.");
        }
        return clips.save(Clip.cut(recording, c.creatorId(), c.creatorNickname(), c.title(), c.offsetSeconds(),
                c.durationSeconds(), clock.instant()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Clip> list(Long targetId, String leagueCode, int page, int size) {
        int safe = size <= 0 ? 20 : Math.min(size, 50);
        return clips.findRecent(targetId, leagueCode, Math.max(page, 0), safe);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Clip> byRecording(Long recordingId) {
        return clips.findByRecording(recordingId);
    }

    @Override
    @Transactional(readOnly = true)
    public Clip get(Long clipId) {
        return clips.findById(clipId).orElseThrow(BroadcastErrors::clipNotFound);
    }

    @Override
    @Transactional
    public void delete(Long clipId, Long requesterId, boolean admin) {
        Clip clip = get(clipId);
        clip.ensureDeletableBy(requesterId, admin);
        clips.delete(clipId);
    }
}
