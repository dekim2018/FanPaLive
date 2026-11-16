package com.livebroadcast.interaction.app.port.out;

import java.time.Instant;
import java.util.List;

import com.livebroadcast.interaction.domain.donation.PaymentApproval;
import com.livebroadcast.interaction.domain.donation.PaymentTransaction;

public interface PaymentGatewayPort {

    /**
     * 결제 승인 요청. 결제사가 거절하면 PaymentRejectedException,
     * 결과를 알 수 없으면(타임아웃, 결제사 5xx) PaymentOutcomeUnknownException 을 던진다.
     */
    PaymentApproval confirm(String paymentKey, String orderId, long amount);

    PaymentApproval fetch(String paymentKey);

    /** [from, to) 사이의 거래 내역(대사용) */
    List<PaymentTransaction> listTransactions(Instant from, Instant to);
}
