package com.livebroadcast.fandom.app.service;

import java.time.Instant;

/** 실시간 순위표에 점수 변화를 반영하는 곳(RankingService). 하트 병합기가 랭킹 구현에 직접 묶이지 않게 분리했다. */
public interface RankingDeltaSink {

    void applyDelta(Long leagueId, Long targetId, long delta, Instant at);
}
