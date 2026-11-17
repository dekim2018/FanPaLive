package com.livebroadcast.fandom.domain.ranking;

/**
 * 랭킹 한 줄.
 *
 * @param rankChange 이전 주 대비 순위 변화(양수 = 상승). 비교할 기록이 없으면 null
 */
public record RankingRow(int rank, Long targetId, String name, String shortName, String colorPrimary,
                         String colorSecondary, String kind, long score, long primaryFans, long followFans,
                         long hearts, long donationAmount, Integer rankChange) {
}
