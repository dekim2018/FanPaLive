package com.livebroadcast.interaction.out.persistence;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.livebroadcast.interaction.app.PageResult;
import com.livebroadcast.interaction.app.port.out.DonationRepositoryPort;
import com.livebroadcast.interaction.domain.donation.Donation;
import com.livebroadcast.interaction.domain.donation.DonationStatus;
import com.livebroadcast.interaction.domain.donation.DonationSummary;
import com.livebroadcast.interaction.domain.donation.DonationTargetSummary;

@Component
public class DonationPersistenceAdapter implements DonationRepositoryPort {

    private static final Sort LATEST = Sort.by(Sort.Direction.DESC, "approvedAt");

    private final DonationJpaRepository repository;

    public DonationPersistenceAdapter(DonationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Donation save(Donation d) {
        return toDomain(repository.save(new DonationJpaEntity(d.getId(), d.getOrderId(), d.getBroadcastId(),
                d.getStreamerId(), d.getStreamerNickname(), d.getDonorId(), d.getDonorNickname(), d.getAmount(),
                d.getMessage(), d.getStatus(), d.getPaymentKey(), d.getApprovedAt(), d.getFailureReason(),
                d.getCreatedAt(), d.getUpdatedAt(), d.getTargetType(), d.getTargetId(), d.getTargetName())));
    }

    @Override
    public Optional<Donation> findByOrderIdForUpdate(String orderId) {
        return repository.findByOrderIdForUpdate(orderId).map(DonationPersistenceAdapter::toDomain);
    }

    @Override
    public PageResult<Donation> findDoneByBroadcast(Long broadcastId, int page, int size) {
        return toPage(repository.findByBroadcastIdAndStatus(broadcastId, DonationStatus.DONE,
                PageRequest.of(page, size, LATEST)));
    }

    @Override
    public DonationSummary summarize(Long broadcastId) {
        return new DonationSummary(broadcastId,
                repository.sumAmount(broadcastId, DonationStatus.DONE),
                repository.countByBroadcastIdAndStatus(broadcastId, DonationStatus.DONE));
    }

    @Override
    public List<Donation> findUnknownUpdatedBefore(Instant before, int limit) {
        return repository.findByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(DonationStatus.UNKNOWN, before,
                PageRequest.of(0, limit)).stream().map(DonationPersistenceAdapter::toDomain).toList();
    }

    @Override
    public PageResult<Donation> findDoneByDonor(Long donorId, int page, int size) {
        return toPage(repository.findByDonorIdAndStatus(donorId, DonationStatus.DONE,
                PageRequest.of(page, size, LATEST)));
    }

    @Override
    public PageResult<Donation> findDoneByStreamer(Long streamerId, int page, int size) {
        return toPage(repository.findByStreamerIdAndStatus(streamerId, DonationStatus.DONE,
                PageRequest.of(page, size, LATEST)));
    }

    @Override
    public PageResult<Donation> findDoneByTarget(Long targetId, int page, int size) {
        return toPage(repository.findByTargetIdAndStatus(targetId, DonationStatus.DONE,
                PageRequest.of(page, size, LATEST)));
    }

    @Override
    public DonationTargetSummary summarizeTarget(Long targetId) {
        return new DonationTargetSummary(targetId,
                repository.sumAmountByTarget(targetId, DonationStatus.DONE),
                repository.countByTargetIdAndStatus(targetId, DonationStatus.DONE));
    }

    @Override
    public Map<Long, Long> sumDoneByTarget(Instant from, Instant to) {
        Map<Long, Long> result = new LinkedHashMap<>();
        for (DonationJpaRepository.TargetTotalRow row : repository.sumByTarget(DonationStatus.DONE, from, to)) {
            result.put(row.getTargetId(), row.getAmount() == null ? 0L : row.getAmount());
        }
        return result;
    }

    @Override
    public List<Donation> findDoneApprovedBetween(Instant from, Instant to) {
        return repository.findByStatusAndApprovedAtGreaterThanEqualAndApprovedAtLessThan(DonationStatus.DONE, from,
                to).stream().map(DonationPersistenceAdapter::toDomain).toList();
    }

    private static PageResult<Donation> toPage(Page<DonationJpaEntity> result) {
        return new PageResult<>(result.getContent().stream().map(DonationPersistenceAdapter::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    private static Donation toDomain(DonationJpaEntity e) {
        return new Donation(e.getId(), e.getOrderId(), e.getBroadcastId(), e.getStreamerId(),
                e.getStreamerNickname(), e.getDonorId(), e.getDonorNickname(), e.getAmount(), e.getMessage(),
                e.getStatus(), e.getPaymentKey(), e.getApprovedAt(), e.getFailureReason(), e.getCreatedAt(),
                e.getUpdatedAt(), e.getTargetType(), e.getTargetId(), e.getTargetName());
    }
}
