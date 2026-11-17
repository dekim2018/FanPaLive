package com.livebroadcast.interaction.app.port.out;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/** 스트리머 순위 재료: 응원팀 편파방별 순시청자(Redis HLL)와 후원 합계(DB) */
public interface StreamerStatsPort {

    void recordView(Long targetId, String monthKey, Long streamerId, String streamerNickname, String viewerKey);

    /** 이번 달 그 팀 편파방을 한 번이라도 연 스트리머 */
    Set<Long> streamers(Long targetId, String monthKey);

    Map<Long, Long> uniqueViewers(Long targetId, String monthKey, Collection<Long> streamerIds);

    Map<Long, String> nicknames(Collection<Long> streamerIds);

    /** 그 팀 편파방에서 받은 확정 후원 합계(스트리머별) */
    Map<Long, Long> donations(Long targetId, Instant from, Instant to);
}
