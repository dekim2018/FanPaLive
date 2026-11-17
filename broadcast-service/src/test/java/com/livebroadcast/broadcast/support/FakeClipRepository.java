package com.livebroadcast.broadcast.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.livebroadcast.broadcast.app.PageResult;
import com.livebroadcast.broadcast.app.port.out.ClipRepositoryPort;
import com.livebroadcast.broadcast.domain.Clip;

public class FakeClipRepository implements ClipRepositoryPort {

    public final Map<Long, Clip> byId = new LinkedHashMap<>();
    private long seq;

    @Override
    public Clip save(Clip c) {
        Clip saved = new Clip(c.getId() == null ? ++seq : c.getId(), c.getRecordingId(), c.getBroadcastId(),
                c.getRecordingOwnerId(), c.getMediaPath(), c.getCreatorId(), c.getCreatorNickname(), c.getTitle(),
                c.getOffsetSeconds(), c.getDurationSeconds(), c.getStartAt(), c.getSide(), c.getCreatedAt());
        byId.put(saved.getId(), saved);
        return saved;
    }

    @Override
    public Optional<Clip> findById(Long id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public PageResult<Clip> findRecent(Long targetId, String leagueCode, int page, int size) {
        List<Clip> list = byId.values().stream()
                .filter(c -> targetId == null || targetId.equals(c.getSide().targetId())).toList();
        return new PageResult<>(list, page, size, list.size(), 1);
    }

    @Override
    public List<Clip> findByRecording(Long recordingId) {
        return byId.values().stream().filter(c -> c.getRecordingId().equals(recordingId)).toList();
    }

    @Override
    public long countByRecordingAndCreator(Long recordingId, Long creatorId) {
        return byId.values().stream()
                .filter(c -> c.getRecordingId().equals(recordingId) && c.getCreatorId().equals(creatorId)).count();
    }

    @Override
    public void delete(Long id) {
        byId.remove(id);
    }

    @Override
    public void deleteByRecording(Long recordingId) {
        byId.values().removeIf(c -> c.getRecordingId().equals(recordingId));
    }
}
