package com.livebroadcast.interaction.app.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.livebroadcast.common.error.DomainException;
import com.livebroadcast.interaction.app.PageResult;
import com.livebroadcast.interaction.app.PaymentProperties;
import com.livebroadcast.interaction.app.port.in.DonationUseCase;
import com.livebroadcast.interaction.app.port.out.BroadcastQueryPort;
import com.livebroadcast.interaction.app.port.out.DomainEventPublisherPort;
import com.livebroadcast.interaction.app.port.out.DonationRepositoryPort;
import com.livebroadcast.interaction.app.port.out.FanTargetQueryPort;
import com.livebroadcast.interaction.app.port.out.FanTargetQueryPort.FanTargetInfo;
import com.livebroadcast.interaction.app.port.out.PaymentGatewayPort;
import com.livebroadcast.interaction.app.port.out.PaymentReconcileLogPort;
import com.livebroadcast.interaction.domain.broadcast.BroadcastInfo;
import com.livebroadcast.interaction.domain.donation.Donation;
import com.livebroadcast.interaction.domain.donation.DonationCompleted;
import com.livebroadcast.interaction.domain.donation.DonationErrors;
import com.livebroadcast.interaction.domain.donation.DonationSummary;
import com.livebroadcast.interaction.domain.donation.DonationTargetSummary;
import com.livebroadcast.interaction.domain.donation.DonationTargetType;
import com.livebroadcast.interaction.domain.donation.PaymentApproval;
import com.livebroadcast.interaction.domain.donation.PaymentOutcomeUnknownException;
import com.livebroadcast.interaction.domain.donation.PaymentRejectedException;
import com.livebroadcast.interaction.domain.donation.PaymentTransaction;

@Service
public class DonationService implements DonationUseCase {

    private static final Logger log = LoggerFactory.getLogger(DonationService.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    /** 결과를 모르는 주문은 이 시간이 지난 뒤부터 결제 조회로 확정한다(결제사 처리가 끝날 시간을 준다). */
    private static final Duration UNKNOWN_GRACE = Duration.ofSeconds(30);
    /** 결제 조회로도 하루 넘게 확정되지 않으면 사람이 확인하도록 기록만 남긴다. */
    private static final Duration UNKNOWN_ALERT_AFTER = Duration.ofDays(1);
    /** 결제창에 들어가 승인 대기 중인 상태. 이 상태면 아직 실패로 보지 않는다. */
    private static final Set<String> GATEWAY_FAILED = Set.of("CANCELED", "ABORTED", "EXPIRED");

    private final DonationRepositoryPort donationRepository;
    private final BroadcastQueryPort broadcastQuery;
    private final FanTargetQueryPort fanTargetQuery;
    private final PaymentGatewayPort paymentGateway;
    private final DomainEventPublisherPort events;
    private final PaymentReconcileLogPort reconcileLog;
    private final PaymentProperties paymentProperties;
    private final TransactionTemplate tx;
    private final Clock clock;

    public DonationService(DonationRepositoryPort donationRepository, BroadcastQueryPort broadcastQuery,
                           FanTargetQueryPort fanTargetQuery, PaymentGatewayPort paymentGateway,
                           DomainEventPublisherPort events, PaymentReconcileLogPort reconcileLog,
                           PaymentProperties paymentProperties, PlatformTransactionManager transactionManager,
                           Clock clock) {
        this.donationRepository = donationRepository;
        this.broadcastQuery = broadcastQuery;
        this.fanTargetQuery = fanTargetQuery;
        this.paymentGateway = paymentGateway;
        this.events = events;
        this.reconcileLog = reconcileLog;
        this.paymentProperties = paymentProperties;
        this.tx = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    /**
     * 후원 대상은 클라이언트가 보낸 값이 아니라 서버가 조회한 값으로 정한다.
     * 방송 후원은 broadcast-service 의 방송 소유자, 응원기금은 fandom-service 의 공개 중인 응원 대상이다.
     */
    @Override
    @Transactional
    public Donation createOrder(CreateOrderCommand command) {
        String orderId = "don_" + UUID.randomUUID().toString().replace("-", "");
        Instant now = clock.instant();
        Donation donation;
        if (command.targetType() == DonationTargetType.FAN_TARGET) {
            if (command.targetId() == null) {
                throw DonationErrors.targetRequired();
            }
            FanTargetInfo target = fanTargetQuery.findTarget(command.targetId())
                    .filter(FanTargetInfo::isActive)
                    .orElseThrow(DonationErrors::invalidFanTarget);
            donation = Donation.orderForTarget(orderId, target.id(), target.name(), command.donorId(),
                    command.donorNickname(), command.amount(), command.message(), paymentProperties.minAmount(),
                    paymentProperties.maxAmount(), now);
        } else {
            if (command.broadcastId() == null) {
                throw DomainException.badRequest("BROADCAST_REQUIRED", "후원할 방송을 선택해 주세요.");
            }
            BroadcastInfo broadcast = broadcastQuery.findBroadcast(command.broadcastId())
                    .orElseThrow(() -> DomainException.notFound("BROADCAST_NOT_FOUND", "방송을 찾을 수 없습니다."));
            donation = Donation.order(orderId, broadcast.id(), broadcast.ownerId(), broadcast.ownerNickname(),
                    broadcast.targetId(), broadcast.targetName(), command.donorId(), command.donorNickname(),
                    command.amount(), command.message(), paymentProperties.minAmount(),
                    paymentProperties.maxAmount(), now);
        }
        return donationRepository.save(donation);
    }

    /**
     * 결제 승인 확정. 같은 주문에 대한 요청은 행 잠금으로 직렬화되고,
     * 이미 같은 paymentKey 로 확정된 주문은 결제사를 다시 호출하지 않고 그대로 돌려준다(중복 적립 방지).
     * 결제사 응답을 받지 못하면 실패로 처리하지 않고 UNKNOWN 으로 남긴 뒤 결제 조회로 확정한다.
     */
    @Override
    @Transactional(noRollbackFor = PaymentRejectedException.class)
    public Donation confirm(ConfirmCommand command) {
        Donation donation = donationRepository.findByOrderIdForUpdate(command.orderId())
                .orElseThrow(DonationErrors::notFound);
        donation.ensureDonor(command.donorId());
        if (donation.isAlreadyApprovedWith(command.paymentKey())) {
            return donation;
        }
        donation.ensureConfirmable();
        donation.ensureRequestedAmount(command.amount());

        PaymentApproval approval;
        try {
            approval = paymentGateway.confirm(command.paymentKey(), command.orderId(), command.amount());
        } catch (PaymentRejectedException e) {
            donation.fail(e.getCode() + ": " + e.getMessage(), clock.instant());
            donationRepository.save(donation);
            throw e;
        } catch (PaymentOutcomeUnknownException e) {
            log.warn("결제 결과를 알 수 없어 UNKNOWN 으로 둡니다 orderId={} reason={}", command.orderId(), e.getMessage());
            donation.markUnknown(command.paymentKey(), clock.instant());
            return donationRepository.save(donation);
        }
        return applyApproval(donation, approval, false);
    }

    @Override
    @Transactional
    public void cancel(String orderId, Long donorId, String reason) {
        Donation donation = donationRepository.findByOrderIdForUpdate(orderId).orElseThrow(DonationErrors::notFound);
        donation.ensureDonor(donorId);
        donation.fail(reason == null ? "USER_CANCELED" : reason, clock.instant());
        donationRepository.save(donation);
    }

    @Override
    @Transactional
    public void handleWebhook(String paymentKey, String orderId) {
        if (paymentKey == null || orderId == null) {
            return;
        }
        Donation donation = donationRepository.findByOrderIdForUpdate(orderId).orElse(null);
        if (donation == null || donation.isAlreadyApprovedWith(paymentKey)) {
            return;
        }
        if (donation.getStatus().isCompleted()) {
            // 오류로 응답하면 결제사가 같은 웹훅을 계속 재전송하므로, 기록만 남기고 정상 응답한다.
            log.warn("이미 다른 결제로 확정된 주문의 웹훅 orderId={} paymentKey={}", orderId, paymentKey);
            return;
        }
        PaymentApproval approval = paymentGateway.fetch(paymentKey);
        if (!approval.isDone()) {
            log.info("웹훅 수신, 아직 승인되지 않은 결제 orderId={} status={}", orderId, approval.status());
            return;
        }
        if (!donation.getStatus().acceptsConfirmRequest()) {
            log.warn("실패/취소된 주문의 결제 완료 웹훅, 결제사 기준으로 적립 orderId={} status={}",
                    orderId, donation.getStatus());
        }
        applyApproval(donation, approval, true);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Donation> listDone(Long broadcastId, int page, int size) {
        return donationRepository.findDoneByBroadcast(broadcastId, Math.max(page, 0), safeSize(size));
    }

    @Override
    @Transactional(readOnly = true)
    public DonationSummary summary(Long broadcastId) {
        return donationRepository.summarize(broadcastId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Donation> listMine(Long donorId, int page, int size) {
        return donationRepository.findDoneByDonor(donorId, Math.max(page, 0), safeSize(size));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Donation> listReceived(Long streamerId, int page, int size) {
        return donationRepository.findDoneByStreamer(streamerId, Math.max(page, 0), safeSize(size));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Donation> listForTarget(Long targetId, int page, int size) {
        return donationRepository.findDoneByTarget(targetId, Math.max(page, 0), safeSize(size));
    }

    @Override
    @Transactional(readOnly = true)
    public DonationTargetSummary targetSummary(Long targetId) {
        return donationRepository.summarizeTarget(targetId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> targetTotals(LocalDate date) {
        return donationRepository.sumDoneByTarget(startOf(date), startOf(date.plusDays(1)));
    }

    /**
     * 결과를 모르는 주문을 결제 조회 API 로 확정한다. 주문마다 따로 트랜잭션을 써서 하나가 실패해도 나머지는 처리된다.
     */
    @Override
    public int resolveUnknown() {
        Instant now = clock.instant();
        List<Donation> unknown = donationRepository.findUnknownUpdatedBefore(now.minus(UNKNOWN_GRACE), 50);
        int resolved = 0;
        for (Donation candidate : unknown) {
            try {
                Boolean done = tx.execute(status -> resolveOne(candidate.getOrderId()));
                if (Boolean.TRUE.equals(done)) {
                    resolved++;
                }
            } catch (RuntimeException e) {
                log.warn("결제 결과 확인 실패(다음 주기에 재시도) orderId={} reason={}", candidate.getOrderId(),
                        e.getMessage());
            }
        }
        return resolved;
    }

    private boolean resolveOne(String orderId) {
        Donation donation = donationRepository.findByOrderIdForUpdate(orderId).orElse(null);
        if (donation == null || !donation.getStatus().isUnknown() || donation.getPaymentKey() == null) {
            return false;
        }
        PaymentApproval approval = paymentGateway.fetch(donation.getPaymentKey());
        Instant now = clock.instant();
        if (approval.isDone()) {
            applyApproval(donation, approval, true);
            log.info("UNKNOWN 주문을 결제 조회로 확정(적립) orderId={}", orderId);
            return true;
        }
        if (GATEWAY_FAILED.contains(approval.status())) {
            donation.resolveAsFailed("GATEWAY_" + approval.status(), now);
            donationRepository.save(donation);
            log.info("UNKNOWN 주문을 결제 조회로 확정(실패) orderId={} status={}", orderId, approval.status());
            return true;
        }
        if (donation.getUpdatedAt().plus(UNKNOWN_ALERT_AFTER).isBefore(now)) {
            reconcileLog.record(orderId, "UNKNOWN_TOO_LONG", "status=" + approval.status(), now);
        }
        return false;
    }

    /**
     * 결제사 거래 내역과 비교한다.
     * - 결제사는 DONE 인데 우리는 아직 확정 전: 결제사 기준으로 적립(자동 보정)
     * - 우리는 확정했는데 결제사는 취소 또는 내역 없음: 돈 문제라 자동으로 고치지 않고 기록만 남긴다(사람이 확인).
     */
    @Override
    public int reconcileWithGateway(LocalDate date) {
        Instant from = startOf(date);
        Instant to = startOf(date.plusDays(1));
        List<PaymentTransaction> transactions = paymentGateway.listTransactions(from, to);
        Instant now = clock.instant();
        int fixed = 0;
        Set<String> gatewayDone = new HashSet<>();
        for (PaymentTransaction t : transactions) {
            if (t.orderId() == null || !t.orderId().startsWith("don_")) {
                continue;
            }
            if (t.isDone()) {
                gatewayDone.add(t.orderId());
                Boolean applied = tx.execute(status -> {
                    Donation d = donationRepository.findByOrderIdForUpdate(t.orderId()).orElse(null);
                    if (d == null || d.getStatus().isCompleted()) {
                        return false;
                    }
                    applyApproval(d, paymentGateway.fetch(t.paymentKey()), true);
                    reconcileLog.record(t.orderId(), "FIXED_FROM_GATEWAY", "was " + d.getStatus(), now);
                    return true;
                });
                if (Boolean.TRUE.equals(applied)) {
                    fixed++;
                }
            } else if (t.isCanceled()) {
                reconcileLog.record(t.orderId(), "CANCELED_AFTER_DONE", "gateway status=" + t.status(), now);
            }
        }
        for (Donation d : donationRepository.findDoneApprovedBetween(from, to)) {
            if (!gatewayDone.contains(d.getOrderId())) {
                reconcileLog.record(d.getOrderId(), "MISSING_IN_GATEWAY", "amount=" + d.getAmount(), now);
            }
        }
        return fixed;
    }

    private Donation applyApproval(Donation donation, PaymentApproval approval, boolean fromGateway) {
        boolean newlyApproved = fromGateway
                ? donation.reconcile(approval, clock.instant())
                : donation.approve(approval, clock.instant());
        Donation saved = donationRepository.save(donation);
        if (newlyApproved) {
            log.info("후원 확정 orderId={} amount={} type={} broadcastId={} targetId={}", saved.getOrderId(),
                    saved.getAmount(), saved.getTargetType(), saved.getBroadcastId(), saved.getTargetId());
            events.publish(new DonationCompleted(saved));
        }
        return saved;
    }

    private static int safeSize(int size) {
        return size <= 0 ? 20 : Math.min(size, 100);
    }

    private static Instant startOf(LocalDate date) {
        return date.atStartOfDay(KST).toInstant();
    }
}
