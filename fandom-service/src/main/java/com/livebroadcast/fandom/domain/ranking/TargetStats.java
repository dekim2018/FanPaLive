package com.livebroadcast.fandom.domain.ranking;

/** 대상의 누적 통계(기간과 무관한 현재 값) */
public record TargetStats(Long targetId, long primaryFans, long followFans, long hearts, long donationTotal) {

    public static TargetStats empty(Long targetId) {
        return new TargetStats(targetId, 0, 0, 0, 0);
    }

    public long totalFans() {
        return primaryFans + followFans;
    }
}
