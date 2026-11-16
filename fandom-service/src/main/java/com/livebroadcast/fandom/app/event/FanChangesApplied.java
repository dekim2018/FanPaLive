package com.livebroadcast.fandom.app.event;

import java.time.Instant;
import java.util.List;

import com.livebroadcast.fandom.domain.fan.FanChange;

/** 팬 수 변화가 DB 에 커밋됨. 커밋 뒤에 실시간 순위표에 반영한다. */
public record FanChangesApplied(Long leagueId, List<FanChange> changes, Instant at) {
}
