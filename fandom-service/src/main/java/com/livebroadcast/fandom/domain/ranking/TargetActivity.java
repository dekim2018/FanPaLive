package com.livebroadcast.fandom.domain.ranking;

import java.time.Instant;

/**
 * 기간 안의 대상별 활동 합계(DB 기준). 랭킹 재계산과 정렬 기준(하트, 후원) 계산에 쓴다.
 *
 * @param lastActivityAt 기간 안에서 마지막으로 점수가 바뀐 시각(동점 순서 복원용). 활동이 없으면 null
 */
public record TargetActivity(Long targetId, long hearts, long heartPoints, long donationAmount,
                             Instant lastActivityAt) {

    public static TargetActivity empty(Long targetId) {
        return new TargetActivity(targetId, 0, 0, 0, null);
    }
}
