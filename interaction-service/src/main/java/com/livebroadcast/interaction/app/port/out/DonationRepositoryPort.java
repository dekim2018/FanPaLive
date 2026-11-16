package com.livebroadcast.interaction.app.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.livebroadcast.interaction.app.PageResult;
import com.livebroadcast.interaction.domain.donation.Donation;
import com.livebroadcast.interaction.domain.donation.DonationSummary;
import com.livebroadcast.interaction.domain.donation.DonationTargetSummary;

public interface DonationRepositoryPort {

    Donation save(Donation donation);

    /** 같은 주문에 대한 승인 요청이 동시에 들어와도 하나씩 처리되도록 행 잠금으로 조회한다. */
    Optional<Donation> findByOrderIdForUpdate(String orderId);

    PageResult<Donation> findDoneByBroadcast(Long broadcastId, int page, int size);

    DonationSummary summarize(Long broadcastId);

    /** 결과를 모르는(UNKNOWN) 주문 중 마지막 변경이 before 보다 오래된 것 */
    List<Donation> findUnknownUpdatedBefore(Instant before, int limit);

    PageResult<Donation> findDoneByDonor(Long donorId, int page, int size);

    PageResult<Donation> findDoneByStreamer(Long streamerId, int page, int size);

    PageResult<Donation> findDoneByTarget(Long targetId, int page, int size);

    DonationTargetSummary summarizeTarget(Long targetId);

    /** [from, to) 에 확정된 후원의 응원 대상별 합계(팬덤 집계 대사용) */
    Map<Long, Long> sumDoneByTarget(Instant from, Instant to);

    /** [from, to) 에 확정된 후원(결제사 대사용) */
    List<Donation> findDoneApprovedBetween(Instant from, Instant to);
}
