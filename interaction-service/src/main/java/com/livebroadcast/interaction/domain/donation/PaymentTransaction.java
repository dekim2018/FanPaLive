package com.livebroadcast.interaction.domain.donation;

/** 결제사 거래 내역 한 건(대사용). status: DONE, CANCELED, PARTIAL_CANCELED 등 */
public record PaymentTransaction(String paymentKey, String orderId, String status, long amount) {

    public boolean isDone() {
        return "DONE".equals(status);
    }

    public boolean isCanceled() {
        return status != null && status.contains("CANCELED");
    }
}
