package com.livebroadcast.broadcast.app.port.out;

import java.util.List;
import java.util.Optional;

import com.livebroadcast.broadcast.app.PageResult;
import com.livebroadcast.broadcast.domain.Clip;

public interface ClipRepositoryPort {

    Clip save(Clip clip);

    Optional<Clip> findById(Long id);

    /** 최신순. targetId, leagueCode 가 null 이면 조건에서 뺀다 */
    PageResult<Clip> findRecent(Long targetId, String leagueCode, int page, int size);

    List<Clip> findByRecording(Long recordingId);

    long countByRecordingAndCreator(Long recordingId, Long creatorId);

    void delete(Long id);

    void deleteByRecording(Long recordingId);
}
