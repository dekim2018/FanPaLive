package com.livebroadcast.fandom.app.port.out;

import java.time.LocalDate;

import com.livebroadcast.fandom.domain.heart.HeartPolicy;
import com.livebroadcast.fandom.domain.heart.LimitDecision;

/**
 * 하트 일일 한도. 검사와 차감을 원자적으로 한다(연타, 동시 요청에도 한도를 넘지 않음).
 */
public interface HeartLimiterPort {

    LimitDecision tryConsume(Long userId, Long targetId, LocalDate date, int amount, HeartPolicy policy);

    /** 한도 차감 후 기록이 실패했을 때 되돌린다. */
    void refund(Long userId, Long targetId, LocalDate date, int amount);

    int remainingToday(Long userId, LocalDate date, HeartPolicy policy);
}
