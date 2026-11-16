package com.livebroadcast.fandom.app.port.out;

import java.time.Instant;
import java.util.List;

import com.livebroadcast.fandom.domain.heart.HeartGrant;
import com.livebroadcast.fandom.domain.ranking.Supporter;

public interface HeartLogPort {

    /** 모은 하트를 한 번의 배치 INSERT 로 기록한다. */
    void insertBatch(List<HeartGrant> grants);

    /** 기간 안에서 하트 점수 + 후원 점수가 높은 팬 */
    List<Supporter> findTopSupporters(Long targetId, Instant from, Instant to, int limit);
}
