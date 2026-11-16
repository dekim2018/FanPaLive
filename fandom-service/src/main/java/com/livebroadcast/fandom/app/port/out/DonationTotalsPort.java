package com.livebroadcast.fandom.app.port.out;

import java.time.LocalDate;
import java.util.Map;

/** 결제 원본(interaction-service)에서 하루 동안 확정된 대상별 후원 합계 */
public interface DonationTotalsPort {

    Map<Long, Long> findTargetTotals(LocalDate date);
}
