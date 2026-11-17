package com.livebroadcast.broadcast.out.persistence;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.livebroadcast.broadcast.domain.RecordingStatus;

public interface RecordingJpaRepository extends JpaRepository<RecordingJpaEntity, Long>,
        JpaSpecificationExecutor<RecordingJpaEntity> {

    Optional<RecordingJpaEntity> findFirstByMediaPathAndStatusOrderByIdDesc(String mediaPath, RecordingStatus status);

    Optional<RecordingJpaEntity> findFirstByMediaPathAndStartedAtBeforeOrderByStartedAtDesc(String mediaPath,
                                                                                            Instant startedAt);

    @Query("select count(b) > 0 from BroadcastJpaEntity b where b.id = :broadcastId and b.moderatedAt is not null")
    boolean isBroadcastModerated(@Param("broadcastId") Long broadcastId);

    boolean existsByMediaPathAndStatus(String mediaPath, RecordingStatus status);
}
