package com.livebroadcast.interaction.app.port.out;

import java.util.Optional;

/** fandom-service 에서 응원 대상(구단/선수)을 확인한다. 응원기금 후원 대상 검증에 쓴다. */
public interface FanTargetQueryPort {

    Optional<FanTargetInfo> findTarget(Long targetId);

    record FanTargetInfo(Long id, String name, String status) {
        public boolean isActive() {
            return "ACTIVE".equals(status);
        }
    }
}
