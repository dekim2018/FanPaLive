package com.livebroadcast.interaction.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.livebroadcast.interaction.domain.donation.DonationStatus;

import jakarta.persistence.LockModeType;

public interface DonationJpaRepository extends JpaRepository<DonationJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DonationJpaEntity d where d.orderId = :orderId")
    Optional<DonationJpaEntity> findByOrderIdForUpdate(@Param("orderId") String orderId);

    Page<DonationJpaEntity> findByBroadcastIdAndStatus(Long broadcastId, DonationStatus status, Pageable pageable);

    Page<DonationJpaEntity> findByDonorIdAndStatus(Long donorId, DonationStatus status, Pageable pageable);

    Page<DonationJpaEntity> findByStreamerIdAndStatus(Long streamerId, DonationStatus status, Pageable pageable);

    Page<DonationJpaEntity> findByTargetIdAndStatus(Long targetId, DonationStatus status, Pageable pageable);

    List<DonationJpaEntity> findByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(DonationStatus status, Instant before,
                                                                              Pageable pageable);

    List<DonationJpaEntity> findByStatusAndApprovedAtGreaterThanEqualAndApprovedAtLessThan(DonationStatus status,
                                                                                          Instant from, Instant to);

    @Query("select coalesce(sum(d.amount), 0L) from DonationJpaEntity d "
            + "where d.broadcastId = :broadcastId and d.status = :status")
    long sumAmount(@Param("broadcastId") Long broadcastId, @Param("status") DonationStatus status);

    long countByBroadcastIdAndStatus(Long broadcastId, DonationStatus status);

    @Query("select coalesce(sum(d.amount), 0L) from DonationJpaEntity d "
            + "where d.targetId = :targetId and d.status = :status")
    long sumAmountByTarget(@Param("targetId") Long targetId, @Param("status") DonationStatus status);

    long countByTargetIdAndStatus(Long targetId, DonationStatus status);

    @Query("select d.targetId as targetId, sum(d.amount) as amount from DonationJpaEntity d "
            + "where d.status = :status and d.targetId is not null "
            + "and d.approvedAt >= :from and d.approvedAt < :to group by d.targetId")
    List<TargetTotalRow> sumByTarget(@Param("status") DonationStatus status, @Param("from") Instant from,
                                     @Param("to") Instant to);

    interface TargetTotalRow {
        Long getTargetId();

        Long getAmount();
    }
}
