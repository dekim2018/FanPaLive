package com.livebroadcast.fandom.app.port.out;

import java.util.Map;

/**
 * 실시간 순위표(Redis ZSET). 원본은 DB 이고, 이 순위표는 매일 DB 기준으로 다시 만든다.
 */
public interface RankingBoardPort {

    /** 점수를 delta 만큼 바꾸고 "이 점수에 도달한 시각"을 지금으로 기록한다(원자적). */
    void applyDelta(Long leagueId, String periodKey, Long targetId, long delta, long secondsSinceSeasonStart);

    /** targetId -> encoded score */
    Map<Long, Double> read(Long leagueId, String periodKey);

    /** 순위표 전체를 한 번에 바꾼다(재계산 중에도 읽기는 이전 값 또는 새 값만 보게). */
    void replace(Long leagueId, String periodKey, Map<Long, Double> encodedScores, long ttlSeconds);
}
