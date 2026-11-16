package com.livebroadcast.interaction.app.port.out;

import java.time.Instant;

public interface PaymentReconcileLogPort {

    /**
     * @param issue FIXED_FROM_GATEWAY(결제 원본 기준으로 적립), CANCELED_AFTER_DONE(확정 뒤 결제 취소, 사람 확인 필요),
     *              MISSING_IN_GATEWAY(우리만 확정, 사람 확인 필요)
     */
    void record(String orderId, String issue, String detail, Instant at);
}
