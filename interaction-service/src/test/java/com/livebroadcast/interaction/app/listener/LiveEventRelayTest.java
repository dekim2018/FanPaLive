package com.livebroadcast.interaction.app.listener;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.livebroadcast.interaction.app.PageResult;
import com.livebroadcast.interaction.app.port.out.DonationRepositoryPort;
import com.livebroadcast.interaction.domain.comment.Comment;
import com.livebroadcast.interaction.domain.comment.CommentDeleted;
import com.livebroadcast.interaction.domain.comment.CommentWritten;
import com.livebroadcast.interaction.domain.donation.Donation;
import com.livebroadcast.interaction.domain.donation.DonationCompleted;
import com.livebroadcast.interaction.domain.donation.DonationSummary;
import com.livebroadcast.interaction.domain.donation.DonationTargetSummary;

/** 도메인 이벤트가 화면이 기대하는 실시간 이벤트 이름과 내용으로 바뀌는지 검증한다. */
class LiveEventRelayTest {

    private static final Instant NOW = Instant.parse("2026-11-10T00:00:00Z");

    record Sent(Long broadcastId, String name, Object payload) {
    }

    private final List<Sent> sent = new ArrayList<>();
    private final LiveEventRelay relay = new LiveEventRelay(
            (broadcastId, name, payload) -> sent.add(new Sent(broadcastId, name, payload)),
            new SummaryOnlyRepository());

    @Test
    void 댓글_작성과_삭제() {
        Comment comment = Comment.write(10L, 2L, "시청자", "안녕하세요", NOW);

        relay.on(new CommentWritten(comment));
        relay.on(new CommentDeleted(5L, 10L));

        assertThat(sent).extracting(Sent::name).containsExactly("comment.created", "comment.deleted");
        assertThat(sent.get(0).payload()).isInstanceOf(LiveEventRelay.CommentPayload.class);
        assertThat(sent.get(1).payload()).isEqualTo(Map.of("id", 5L));
    }

    @Test
    void 후원_확정이면_내역과_합계를_함께_보낸다() {
        Donation donation = Donation.order("don_1", 10L, 1L, "방송자", 2L, "시청자", 5000, "화이팅",
                1000, 1_000_000, NOW);

        relay.on(new DonationCompleted(donation));

        assertThat(sent).extracting(Sent::name).containsExactly("donation.completed", "donation.summary");
        assertThat(sent.get(1).payload()).isEqualTo(new DonationSummary(10L, 5000, 1));
    }

    @Test
    void 구단_응원기금은_방송_화면으로_보내지_않는다() {
        Donation donation = Donation.orderForTarget("don_t", 1L, "두산 베어스", 2L, "시청자", 5000, "", 1000,
                1_000_000, NOW);

        relay.on(new DonationCompleted(donation));

        assertThat(sent).isEmpty();
    }

    @Test
    void 확정된_응원_대상_후원만_팬덤_스트림으로_보낸다() {
        List<Donation> published = new ArrayList<>();
        DonationStreamRelay streamRelay = new DonationStreamRelay(published::add);
        Donation plain = Donation.order("don_a", 10L, 1L, "방송자", 2L, "시청자", 5000, "", 1000, 1_000_000, NOW);
        Donation fund = Donation.orderForTarget("don_b", 1L, "두산 베어스", 2L, "시청자", 5000, "", 1000,
                1_000_000, NOW);
        fund.approve(new com.livebroadcast.interaction.domain.donation.PaymentApproval("pk", "don_b", "DONE", 5000,
                NOW), NOW);
        plain.approve(new com.livebroadcast.interaction.domain.donation.PaymentApproval("pk2", "don_a", "DONE",
                5000, NOW), NOW);

        streamRelay.on(new DonationCompleted(plain));
        streamRelay.on(new DonationCompleted(fund));

        assertThat(published).extracting(Donation::getOrderId).containsExactly("don_b");
    }

    static class SummaryOnlyRepository implements DonationRepositoryPort {
        @Override
        public Donation save(Donation donation) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Donation> findByOrderIdForUpdate(String orderId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<Donation> findDoneByBroadcast(Long broadcastId, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DonationSummary summarize(Long broadcastId) {
            return new DonationSummary(broadcastId, 5000, 1);
        }

        @Override
        public List<Donation> findUnknownUpdatedBefore(Instant before, int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<Donation> findDoneByDonor(Long donorId, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<Donation> findDoneByStreamer(Long streamerId, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<Donation> findDoneByTarget(Long targetId, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DonationTargetSummary summarizeTarget(Long targetId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<Long, Long> sumDoneByTarget(Instant from, Instant to) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Donation> findDoneApprovedBetween(Instant from, Instant to) {
            throw new UnsupportedOperationException();
        }
    }
}
