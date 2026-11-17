package com.livebroadcast.broadcast.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClipJpaRepository extends JpaRepository<ClipJpaEntity, Long>,
        JpaSpecificationExecutor<ClipJpaEntity> {

    List<ClipJpaEntity> findTop50ByRecordingIdOrderByIdDesc(Long recordingId);

    long countByRecordingIdAndCreatorId(Long recordingId, Long creatorId);

    @Modifying
    @Query("delete from ClipJpaEntity c where c.recordingId = :recordingId")
    int deleteByRecording(@Param("recordingId") Long recordingId);
}
