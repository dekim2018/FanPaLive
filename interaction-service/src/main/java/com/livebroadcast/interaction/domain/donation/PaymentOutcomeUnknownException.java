package com.livebroadcast.interaction.domain.donation;

import com.livebroadcast.common.error.DomainException;
import com.livebroadcast.common.error.ErrorType;

/**
 * 결제사에 승인을 요청했지만 결과를 받지 못함(응답 타임아웃, 연결 끊김, 결제사 5xx).
 * 결제가 됐을 수도, 안 됐을 수도 있으므로 실패로 처리하지 않고 UNKNOWN 상태로 둔다.
 */
public class PaymentOutcomeUnknownException extends DomainException {

    public PaymentOutcomeUnknownException(String message) {
        super(ErrorType.BAD_GATEWAY, "PAYMENT_OUTCOME_UNKNOWN",
                message == null ? "결제 결과를 확인하는 중입니다." : message);
    }
}
