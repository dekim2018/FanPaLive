package com.livebroadcast.fandom.domain.heart;

import java.time.Instant;
import java.time.LocalDate;

import com.livebroadcast.fandom.domain.match.MatchSide;

/**
 * 한도 검사를 통과한 하트 한 건. 요청 스레드는 이것을 큐에 넣고 바로 응답하고,
 * 실제 DB 기록과 랭킹 반영은 200ms 마다 모아서 한 번에 한다(쓰기 병합).
 */
public record HeartGrant(Long userId, String userNickname, Long targetId, Long leagueId, Long matchId,
                         MatchSide matchSide, int amount, int points, boolean suspicious, HeartSource source,
                         LocalDate givenDate, Instant createdAt) {

    /** 이 하트가 랭킹에 더하는 점수 */
    public long rankingPoints() {
        return suspicious ? 0 : (long) amount * points;
    }

    /** 랭킹 하트 수에 들어가는 개수(의심 하트 제외) */
    public int countedHearts() {
        return suspicious ? 0 : amount;
    }
}
