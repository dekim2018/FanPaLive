package com.livebroadcast.fandom.domain.ranking;

import java.time.Instant;
import java.time.LocalDate;

/** 하트를 모아서 한 번에 반영할 때의 대상별, 날짜별 증가분 */
public record HeartDailyDelta(Long targetId, LocalDate date, long hearts, long heartPoints, long suspiciousHearts,
                              Instant lastAt) {
}
