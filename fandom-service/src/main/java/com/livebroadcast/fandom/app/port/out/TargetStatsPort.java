package com.livebroadcast.fandom.app.port.out;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.livebroadcast.fandom.domain.fan.FanChange;
import com.livebroadcast.fandom.domain.ranking.HeartDailyDelta;
import com.livebroadcast.fandom.domain.ranking.TargetActivity;
import com.livebroadcast.fandom.domain.ranking.TargetStats;

/**
 * 대상별 통계. 원장(heart_logs, donation_contributions)과 별도로 조회용 집계를 유지한다(읽기 모델 분리).
 */
public interface TargetStatsPort {

    void applyFanChanges(List<FanChange> changes, Instant at);

    void addHearts(List<HeartDailyDelta> deltas);

    void addDonation(Long targetId, LocalDate date, long amount, Instant at);

    /** 대사 결과로 하루 후원 합계를 결제 원본 기준 값으로 맞춘다. 차이(새 값 - 기존 값)를 돌려준다. */
    long overwriteDailyDonation(Long targetId, LocalDate date, long amount);

    Map<Long, TargetStats> findStats(Collection<Long> targetIds);

    /** [from, toExclusive) 기간의 대상별 활동 합계 */
    Map<Long, TargetActivity> findActivities(Collection<Long> targetIds, LocalDate from, LocalDate toExclusive);

    Map<Long, Long> findDailyDonations(LocalDate date);

    /** 종목 통합 팬 수 상위 대상 */
    List<Long> findTopByFans(int limit);
}
