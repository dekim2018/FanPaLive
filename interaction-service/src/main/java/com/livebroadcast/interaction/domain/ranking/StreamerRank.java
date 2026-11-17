package com.livebroadcast.interaction.domain.ranking;

/**
 * 응원팀별 월간 스트리머 순위 한 줄.
 *
 * @param uniqueViewers  그 팀 편파방에서 이번 달 모은 순시청자(HyperLogLog 근사치)
 * @param donationAmount 그 팀 편파방에서 이번 달 받은 확정 후원 합계
 */
public record StreamerRank(int rank, Long streamerId, String nickname, long uniqueViewers, long donationAmount) {

    /** 정렬 점수: 순시청자 1명 = 1점, 후원 1,000원 = 1점. 돈만으로 순위가 정해지지 않게 시청자 비중을 크게 둔다. */
    public long score() {
        return uniqueViewers + donationAmount / 1_000;
    }
}
