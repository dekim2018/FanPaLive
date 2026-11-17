package com.livebroadcast.fandom.app.port.in;

import java.time.Instant;

import com.livebroadcast.fandom.domain.heart.HeartResult;
import com.livebroadcast.fandom.domain.heart.HeartSource;

public interface HeartUseCase {

    HeartResult give(GiveCommand command);

    Remaining remaining(Long userId);

    /**
     * @param deviceId 브라우저가 만든 기기 식별자(로컬 저장소). 원문은 저장하지 않고 해시만 쓴다.
     * @param clientIp 요청 IP. 원문은 저장하지 않고 해시만 쓴다.
     */
    record GiveCommand(Long userId, String nickname, Instant joinedAt, Long targetId, int amount, HeartSource source,
                       Long matchId, String deviceId, String clientIp) {
    }

    record Remaining(int dailyMax, int perTargetMax, int remainingToday) {
    }
}
