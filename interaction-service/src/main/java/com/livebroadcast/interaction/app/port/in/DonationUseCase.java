package com.livebroadcast.interaction.app.port.in;

import java.time.LocalDate;
import java.util.Map;

import com.livebroadcast.interaction.app.PageResult;
import com.livebroadcast.interaction.domain.donation.Donation;
import com.livebroadcast.interaction.domain.donation.DonationSummary;
import com.livebroadcast.interaction.domain.donation.DonationTargetSummary;
import com.livebroadcast.interaction.domain.donation.DonationTargetType;

public interface DonationUseCase {

    Donation createOrder(CreateOrderCommand command);

    Donation confirm(ConfirmCommand command);

    void cancel(String orderId, Long donorId, String reason);

    /** 결제사 웹훅. 본문은 신뢰하지 않고 paymentKey 로 결제사에 다시 조회해서 처리한다. */
    void handleWebhook(String paymentKey, String orderId);

    PageResult<Donation> listDone(Long broadcastId, int page, int size);

    DonationSummary summary(Long broadcastId);

    PageResult<Donation> listMine(Long donorId, int page, int size);

    PageResult<Donation> listReceived(Long streamerId, int page, int size);

    PageResult<Donation> listForTarget(Long targetId, int page, int size);

    DonationTargetSummary targetSummary(Long targetId);

    /** 하루(KST) 동안 확정된 후원의 응원 대상별 합계. 팬덤 서비스의 새벽 대사가 호출한다. */
    Map<Long, Long> targetTotals(LocalDate date);

    /** 결과를 모르는 주문을 결제 조회로 확정한다. 확정한 주문 수를 돌려준다. */
    int resolveUnknown();

    /** 하루(KST) 동안의 결제사 거래 내역과 우리 기록을 비교한다. 결제사 기준으로 보정한 수를 돌려준다. */
    int reconcileWithGateway(LocalDate date);

    /**
     * @param targetType BROADCAST 면 broadcastId 필수(방송자 후원), FAN_TARGET 이면 targetId 필수(구단/선수 응원기금)
     */
    record CreateOrderCommand(DonationTargetType targetType, Long broadcastId, Long targetId, Long donorId,
                              String donorNickname, long amount, String message) {

        public CreateOrderCommand(Long broadcastId, Long donorId, String donorNickname, long amount, String message) {
            this(DonationTargetType.BROADCAST, broadcastId, null, donorId, donorNickname, amount, message);
        }
    }

    record ConfirmCommand(Long donorId, String paymentKey, String orderId, long amount) {
    }
}
