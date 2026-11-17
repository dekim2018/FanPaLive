package com.livebroadcast.interaction.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.livebroadcast.interaction.app.PageResult;
import com.livebroadcast.interaction.app.PaymentProperties;
import com.livebroadcast.interaction.app.port.in.DonationUseCase.ConfirmCommand;
import com.livebroadcast.interaction.app.port.in.DonationUseCase.CreateOrderCommand;
import com.livebroadcast.interaction.app.port.out.DonationRepositoryPort;
import com.livebroadcast.interaction.app.port.out.FanTargetQueryPort.FanTargetInfo;
import com.livebroadcast.interaction.app.port.out.PaymentGatewayPort;
import com.livebroadcast.interaction.domain.broadcast.BroadcastInfo;
import com.livebroadcast.interaction.domain.donation.Donation;
import com.livebroadcast.interaction.domain.donation.DonationCompleted;
import com.livebroadcast.interaction.domain.donation.DonationStatus;
import com.livebroadcast.interaction.domain.donation.DonationSummary;
import com.livebroadcast.interaction.domain.donation.DonationTargetSummary;
import com.livebroadcast.interaction.domain.donation.DonationTargetType;
import com.livebroadcast.interaction.domain.donation.PaymentApproval;
import com.livebroadcast.interaction.domain.donation.PaymentOutcomeUnknownException;
import com.livebroadcast.interaction.domain.donation.PaymentRejectedException;
import com.livebroadcast.interaction.domain.donation.PaymentTransaction;
import com.livebroadcast.interaction.support.NoopTransactionManager;

/**
 * 스프링 컨텍스트 없이 포트를 가짜 구현으로 바꿔서 후원 확정 흐름(특히 중복 적립 방지)을 검증한다.
 */
class DonationServiceTest {

    private static final Long STREAMER = 1L;
    private static final Long DONOR = 2L;

    private final FakeDonationRepository repository = new FakeDonationRepository();
    private final FakePaymentGateway gateway = new FakePaymentGateway();
    private final List<Object> events = new ArrayList<>();
    private final List<String> reconcileIssues = new ArrayList<>();
    private Instant now = Instant.parse("2026-11-10T00:00:00Z");
    private DonationService service;

    private static final Long DOOSAN = 1L;

    @BeforeEach
    void setUp() {
        service = new DonationService(
                repository,
                id -> Optional.of(id == 20L
                        ? new BroadcastInfo(id, STREAMER, "방송자", "두산 편파", "LIVE", DOOSAN, "두산 베어스")
                        : new BroadcastInfo(id, STREAMER, "방송자", "방송", "LIVE")),
                id -> id.equals(DOOSAN) ? Optional.of(new FanTargetInfo(DOOSAN, "두산 베어스", "ACTIVE"))
                        : id == 2L ? Optional.of(new FanTargetInfo(2L, "심사중 구단", "PENDING")) : Optional.empty(),
                gateway,
                events::add,
                (orderId, issue, detail, at) -> reconcileIssues.add(orderId + ":" + issue),
                new PaymentProperties("ck", "sk", null, 1000, 1_000_000),
                new NoopTransactionManager(),
                new Clock() {
                    @Override
                    public ZoneOffset getZone() {
                        return ZoneOffset.UTC;
                    }

                    @Override
                    public Clock withZone(java.time.ZoneId zone) {
                        return this;
                    }

                    @Override
                    public Instant instant() {
                        return now;
                    }
                });
    }

    @Test
    void 후원_대상은_방송_소유자로_정해진다() {
        Donation order = service.createOrder(new CreateOrderCommand(10L, DONOR, "시청자", 5000, "화이팅"));

        assertThat(order.getStreamerId()).isEqualTo(STREAMER);
        assertThat(order.getStatus()).isEqualTo(DonationStatus.PENDING);
    }

    @Test
    void 같은_승인_요청이_여러번_와도_한번만_적립된다() {
        Donation order = service.createOrder(new CreateOrderCommand(10L, DONOR, "시청자", 5000, ""));
        ConfirmCommand confirm = new ConfirmCommand(DONOR, "pk-1", order.getOrderId(), 5000);

        service.confirm(confirm);
        service.confirm(confirm);
        service.handleWebhook("pk-1", order.getOrderId());

        assertThat(gateway.confirmCalls.get()).isEqualTo(1);
        assertThat(service.summary(10L).totalAmount()).isEqualTo(5000);
        assertThat(events).filteredOn(DonationCompleted.class::isInstance).hasSize(1);
    }

    @Test
    void 브라우저가_보낸_금액이_주문과_다르면_결제사를_호출하지_않는다() {
        Donation order = service.createOrder(new CreateOrderCommand(10L, DONOR, "시청자", 5000, ""));

        assertThatThrownBy(() -> service.confirm(new ConfirmCommand(DONOR, "pk-1", order.getOrderId(), 100)))
                .hasMessageContaining("금액");
        assertThat(gateway.confirmCalls.get()).isZero();
    }

    @Test
    void 결제사가_거절하면_주문은_FAILED_가_된다() {
        Donation order = service.createOrder(new CreateOrderCommand(10L, DONOR, "시청자", 5000, ""));
        gateway.reject = true;

        assertThatThrownBy(() -> service.confirm(new ConfirmCommand(DONOR, "pk-1", order.getOrderId(), 5000)))
                .isInstanceOf(PaymentRejectedException.class);
        assertThat(repository.byOrderId.get(order.getOrderId()).getStatus()).isEqualTo(DonationStatus.FAILED);
        assertThat(events).isEmpty();
    }

    @Test
    void 실패한_주문은_결제사에_다시_승인을_요청하지_않는다() {
        Donation order = service.createOrder(new CreateOrderCommand(10L, DONOR, "시청자", 5000, ""));
        gateway.reject = true;
        ConfirmCommand confirm = new ConfirmCommand(DONOR, "pk-1", order.getOrderId(), 5000);
        assertThatThrownBy(() -> service.confirm(confirm)).isInstanceOf(PaymentRejectedException.class);

        gateway.reject = false;
        assertThatThrownBy(() -> service.confirm(confirm)).hasMessageContaining("이미 실패했거나 취소된");
        assertThat(gateway.confirmCalls.get()).isEqualTo(1);
    }

    @Test
    void 실패한_주문이라도_결제사가_결제_완료를_알려오면_적립한다() {
        Donation order = service.createOrder(new CreateOrderCommand(10L, DONOR, "시청자", 5000, ""));
        service.cancel(order.getOrderId(), DONOR, null);
        gateway.approved.put("pk-1", new PaymentApproval("pk-1", order.getOrderId(), "DONE", 5000, Instant.now()));

        service.handleWebhook("pk-1", order.getOrderId());

        assertThat(repository.byOrderId.get(order.getOrderId()).getStatus()).isEqualTo(DonationStatus.DONE);
        assertThat(service.summary(10L).totalAmount()).isEqualTo(5000);
        assertThat(events).filteredOn(DonationCompleted.class::isInstance).hasSize(1);
    }

    @Test
    void 다른_사용자는_남의_주문을_승인할_수_없다() {
        Donation order = service.createOrder(new CreateOrderCommand(10L, DONOR, "시청자", 5000, ""));

        assertThatThrownBy(() -> service.confirm(new ConfirmCommand(99L, "pk-1", order.getOrderId(), 5000)))
                .hasMessageContaining("본인");
    }

    @Test
    void 응원기금은_방송자_없이_응원_대상으로_주문된다() {
        Donation order = service.createOrder(new CreateOrderCommand(DonationTargetType.FAN_TARGET, null, DOOSAN,
                DONOR, "시청자", 10000, "두산 화이팅"));

        assertThat(order.getTargetType()).isEqualTo(DonationTargetType.FAN_TARGET);
        assertThat(order.getStreamerId()).isNull();
        assertThat(order.getTargetName()).isEqualTo("두산 베어스");
        assertThat(order.orderName()).contains("두산 베어스");
    }

    @Test
    void 공개되지_않은_응원_대상에는_응원기금을_낼_수_없다() {
        assertThatThrownBy(() -> service.createOrder(new CreateOrderCommand(DonationTargetType.FAN_TARGET, null, 2L,
                DONOR, "시청자", 10000, ""))).hasMessageContaining("응원");
        assertThatThrownBy(() -> service.createOrder(new CreateOrderCommand(DonationTargetType.FAN_TARGET, null,
                null, DONOR, "시청자", 10000, ""))).hasMessageContaining("응원");
    }

    @Test
    void 편파방_후원은_방송자에게_가면서_응원팀_정보도_남긴다() {
        Donation order = service.createOrder(new CreateOrderCommand(20L, DONOR, "시청자", 5000, ""));

        assertThat(order.getStreamerId()).isEqualTo(STREAMER);
        assertThat(order.getTargetId()).isEqualTo(DOOSAN);
        assertThat(order.countsForFanTarget()).isFalse();
        service.confirm(new ConfirmCommand(DONOR, "pk-1", order.getOrderId(), 5000));
        assertThat(repository.byOrderId.get(order.getOrderId()).countsForFanTarget()).isTrue();
    }

    @Test
    void 결제사_응답을_못_받으면_실패가_아니라_UNKNOWN_으로_남긴다() {
        Donation order = service.createOrder(new CreateOrderCommand(10L, DONOR, "시청자", 5000, ""));
        gateway.timeout = true;

        Donation result = service.confirm(new ConfirmCommand(DONOR, "pk-1", order.getOrderId(), 5000));

        assertThat(result.getStatus()).isEqualTo(DonationStatus.UNKNOWN);
        assertThat(result.getPaymentKey()).isEqualTo("pk-1");
        assertThat(events).isEmpty();
    }

    @Test
    void UNKNOWN_주문은_결제_조회로_승인이_확인되면_한번만_적립된다() {
        Donation order = service.createOrder(new CreateOrderCommand(10L, DONOR, "시청자", 5000, ""));
        gateway.timeout = true;
        service.confirm(new ConfirmCommand(DONOR, "pk-1", order.getOrderId(), 5000));
        // 사실 결제사에서는 승인이 끝나 있었다.
        gateway.approved.put("pk-1", new PaymentApproval("pk-1", order.getOrderId(), "DONE", 5000, now));

        assertThat(service.resolveUnknown()).isZero(); // 유예 시간 전에는 건드리지 않는다
        now = now.plusSeconds(60);
        assertThat(service.resolveUnknown()).isEqualTo(1);
        assertThat(service.resolveUnknown()).isZero();

        assertThat(repository.byOrderId.get(order.getOrderId()).getStatus()).isEqualTo(DonationStatus.DONE);
        assertThat(service.summary(10L).totalAmount()).isEqualTo(5000);
        assertThat(events).filteredOn(DonationCompleted.class::isInstance).hasSize(1);
    }

    @Test
    void UNKNOWN_상태에서_사용자가_다시_승인을_눌러도_확정할_수_있다() {
        Donation order = service.createOrder(new CreateOrderCommand(10L, DONOR, "시청자", 5000, ""));
        gateway.timeout = true;
        ConfirmCommand confirm = new ConfirmCommand(DONOR, "pk-1", order.getOrderId(), 5000);
        service.confirm(confirm);

        gateway.timeout = false;
        Donation result = service.confirm(confirm);

        assertThat(result.getStatus()).isEqualTo(DonationStatus.DONE);
        assertThat(events).filteredOn(DonationCompleted.class::isInstance).hasSize(1);
    }

    @Test
    void UNKNOWN_주문이_결제사에서_취소된_것으로_확인되면_FAILED_가_된다() {
        Donation order = service.createOrder(new CreateOrderCommand(10L, DONOR, "시청자", 5000, ""));
        gateway.timeout = true;
        service.confirm(new ConfirmCommand(DONOR, "pk-1", order.getOrderId(), 5000));
        gateway.approved.put("pk-1", new PaymentApproval("pk-1", order.getOrderId(), "ABORTED", 5000, null));
        now = now.plusSeconds(60);

        service.resolveUnknown();

        assertThat(repository.byOrderId.get(order.getOrderId()).getStatus()).isEqualTo(DonationStatus.FAILED);
        assertThat(events).isEmpty();
    }

    @Test
    void 결제사_대사는_놓친_승인을_적립하고_우리만_확정한_건은_기록만_남긴다() {
        Donation missed = service.createOrder(new CreateOrderCommand(10L, DONOR, "시청자", 5000, ""));
        Donation ours = service.createOrder(new CreateOrderCommand(10L, DONOR, "시청자", 3000, ""));
        service.confirm(new ConfirmCommand(DONOR, "pk-ours", ours.getOrderId(), 3000));
        gateway.approved.put("pk-missed",
                new PaymentApproval("pk-missed", missed.getOrderId(), "DONE", 5000, now));
        gateway.transactions.add(new PaymentTransaction("pk-missed", missed.getOrderId(), "DONE", 5000));
        gateway.transactions.add(new PaymentTransaction("pk-other", "shop_1", "DONE", 100));

        int fixed = service.reconcileWithGateway(LocalDate.of(2026, 11, 10));

        assertThat(fixed).isEqualTo(1);
        assertThat(repository.byOrderId.get(missed.getOrderId()).getStatus()).isEqualTo(DonationStatus.DONE);
        assertThat(reconcileIssues).containsExactlyInAnyOrder(missed.getOrderId() + ":FIXED_FROM_GATEWAY",
                ours.getOrderId() + ":MISSING_IN_GATEWAY");
    }

    @Test
    void 하루_응원_대상별_합계에는_응원기금과_편파방_후원이_모두_들어간다() {
        Donation fund = service.createOrder(new CreateOrderCommand(DonationTargetType.FAN_TARGET, null, DOOSAN,
                DONOR, "시청자", 10000, ""));
        Donation side = service.createOrder(new CreateOrderCommand(20L, DONOR, "시청자", 5000, ""));
        Donation plain = service.createOrder(new CreateOrderCommand(10L, DONOR, "시청자", 7000, ""));
        service.confirm(new ConfirmCommand(DONOR, "pk-1", fund.getOrderId(), 10000));
        service.confirm(new ConfirmCommand(DONOR, "pk-2", side.getOrderId(), 5000));
        service.confirm(new ConfirmCommand(DONOR, "pk-3", plain.getOrderId(), 7000));

        assertThat(service.targetTotals(LocalDate.of(2026, 11, 10))).containsEntry(DOOSAN, 15000L).hasSize(1);
        assertThat(service.targetSummary(DOOSAN).totalAmount()).isEqualTo(15000);
    }

    static class FakePaymentGateway implements PaymentGatewayPort {
        final AtomicInteger confirmCalls = new AtomicInteger();
        final Map<String, PaymentApproval> approved = new HashMap<>();
        final List<PaymentTransaction> transactions = new ArrayList<>();
        boolean reject;
        boolean timeout;

        @Override
        public PaymentApproval confirm(String paymentKey, String orderId, long amount) {
            confirmCalls.incrementAndGet();
            if (reject) {
                throw new PaymentRejectedException("REJECT_CARD_PAYMENT", "카드 거절");
            }
            if (timeout) {
                throw new PaymentOutcomeUnknownException("응답 시간 초과");
            }
            PaymentApproval approval = new PaymentApproval(paymentKey, orderId, "DONE", amount, Instant.parse("2026-11-10T00:00:00Z"));
            approved.put(paymentKey, approval);
            return approval;
        }

        @Override
        public PaymentApproval fetch(String paymentKey) {
            PaymentApproval approval = approved.get(paymentKey);
            return approval != null ? approval : new PaymentApproval(paymentKey, null, "IN_PROGRESS", 0, null);
        }

        @Override
        public List<PaymentTransaction> listTransactions(Instant from, Instant to) {
            return transactions;
        }
    }

    static class FakeDonationRepository implements DonationRepositoryPort {
        final Map<String, Donation> byOrderId = new HashMap<>();
        final AtomicLong seq = new AtomicLong();

        @Override
        public Donation save(Donation d) {
            Donation saved = new Donation(d.getId() == null ? seq.incrementAndGet() : d.getId(), d.getOrderId(),
                    d.getBroadcastId(), d.getStreamerId(), d.getStreamerNickname(), d.getDonorId(),
                    d.getDonorNickname(), d.getAmount(), d.getMessage(), d.getStatus(), d.getPaymentKey(),
                    d.getApprovedAt(), d.getFailureReason(), d.getCreatedAt(), d.getUpdatedAt(), d.getTargetType(),
                    d.getTargetId(), d.getTargetName());
            byOrderId.put(saved.getOrderId(), saved);
            return saved;
        }

        @Override
        public Optional<Donation> findByOrderIdForUpdate(String orderId) {
            return Optional.ofNullable(byOrderId.get(orderId));
        }

        @Override
        public PageResult<Donation> findDoneByBroadcast(Long broadcastId, int page, int size) {
            List<Donation> done = byOrderId.values().stream()
                    .filter(d -> broadcastId.equals(d.getBroadcastId()) && d.getStatus() == DonationStatus.DONE)
                    .toList();
            return new PageResult<>(done, 0, size, done.size(), 1);
        }

        @Override
        public DonationSummary summarize(Long broadcastId) {
            List<Donation> done = findDoneByBroadcast(broadcastId, 0, 100).content();
            return new DonationSummary(broadcastId, done.stream().mapToLong(Donation::getAmount).sum(), done.size());
        }
    
        private List<Donation> done() {
            return byOrderId.values().stream().filter(d -> d.getStatus() == DonationStatus.DONE).toList();
        }

        @Override
        public List<Donation> findUnknownUpdatedBefore(Instant before, int limit) {
            return byOrderId.values().stream()
                    .filter(d -> d.getStatus() == DonationStatus.UNKNOWN && d.getUpdatedAt().isBefore(before))
                    .limit(limit).toList();
        }

        @Override
        public PageResult<Donation> findDoneByDonor(Long donorId, int page, int size) {
            List<Donation> list = done().stream().filter(d -> donorId.equals(d.getDonorId())).toList();
            return new PageResult<>(list, 0, size, list.size(), 1);
        }

        @Override
        public PageResult<Donation> findDoneByStreamer(Long streamerId, int page, int size) {
            List<Donation> list = done().stream().filter(d -> streamerId.equals(d.getStreamerId())).toList();
            return new PageResult<>(list, 0, size, list.size(), 1);
        }

        @Override
        public PageResult<Donation> findDoneByTarget(Long targetId, int page, int size) {
            List<Donation> list = done().stream().filter(d -> targetId.equals(d.getTargetId())).toList();
            return new PageResult<>(list, 0, size, list.size(), 1);
        }

        @Override
        public DonationTargetSummary summarizeTarget(Long targetId) {
            List<Donation> list = findDoneByTarget(targetId, 0, 100).content();
            return new DonationTargetSummary(targetId, list.stream().mapToLong(Donation::getAmount).sum(),
                    list.size());
        }

        @Override
        public Map<Long, Long> sumDoneByTarget(Instant from, Instant to) {
            Map<Long, Long> sums = new HashMap<>();
            findDoneApprovedBetween(from, to).stream().filter(Donation::countsForFanTarget)
                    .forEach(d -> sums.merge(d.getTargetId(), d.getAmount(), Long::sum));
            return sums;
        }

        @Override
        public List<Donation> findDoneApprovedBetween(Instant from, Instant to) {
            return done().stream()
                    .filter(d -> !d.getApprovedAt().isBefore(from) && d.getApprovedAt().isBefore(to)).toList();
        }
    }
}
