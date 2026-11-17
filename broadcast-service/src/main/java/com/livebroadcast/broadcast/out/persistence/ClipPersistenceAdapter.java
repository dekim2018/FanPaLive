package com.livebroadcast.broadcast.out.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.livebroadcast.broadcast.app.PageResult;
import com.livebroadcast.broadcast.app.port.out.ClipRepositoryPort;
import com.livebroadcast.broadcast.domain.Clip;

import jakarta.persistence.criteria.Predicate;

@Component
public class ClipPersistenceAdapter implements ClipRepositoryPort {

    private final ClipJpaRepository repository;

    public ClipPersistenceAdapter(ClipJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Clip save(Clip clip) {
        return toDomain(repository.save(new ClipJpaEntity(clip.getId(), clip.getRecordingId(), clip.getBroadcastId(),
                clip.getRecordingOwnerId(), clip.getMediaPath(), clip.getCreatorId(), clip.getCreatorNickname(),
                clip.getTitle(), clip.getOffsetSeconds(), clip.getDurationSeconds(), clip.getStartAt(),
                FanSideColumns.from(clip.getSide()), clip.getCreatedAt())));
    }

    @Override
    public Optional<Clip> findById(Long id) {
        return repository.findById(id).map(ClipPersistenceAdapter::toDomain);
    }

    @Override
    public PageResult<Clip> findRecent(Long targetId, String leagueCode, int page, int size) {
        Specification<ClipJpaEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(ModerationFilter.notModerated(root, query, cb));
            if (targetId != null) {
                predicates.add(cb.equal(root.get("side").get("targetId"), targetId));
            }
            if (leagueCode != null) {
                predicates.add(cb.equal(root.get("side").get("leagueCode"), leagueCode));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<ClipJpaEntity> result = repository.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return new PageResult<>(result.getContent().stream().map(ClipPersistenceAdapter::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public List<Clip> findByRecording(Long recordingId) {
        return repository.findTop50ByRecordingIdOrderByIdDesc(recordingId).stream()
                .map(ClipPersistenceAdapter::toDomain).toList();
    }

    @Override
    public long countByRecordingAndCreator(Long recordingId, Long creatorId) {
        return repository.countByRecordingIdAndCreatorId(recordingId, creatorId);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public void deleteByRecording(Long recordingId) {
        repository.deleteByRecording(recordingId);
    }

    static Clip toDomain(ClipJpaEntity e) {
        return new Clip(e.getId(), e.getRecordingId(), e.getBroadcastId(), e.getRecordingOwnerId(), e.getMediaPath(),
                e.getCreatorId(), e.getCreatorNickname(), e.getTitle(), e.getOffsetSeconds(), e.getDurationSeconds(),
                e.getStartAt(), FanSideColumns.toDomain(e.getSide()), e.getCreatedAt());
    }
}
