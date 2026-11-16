package com.livebroadcast.broadcast.app.port.out;

import java.util.Optional;

/**
 * 응원 대상과 경기를 fandom-service 에서 조회한다.
 * 서비스와 통신이 안 되면 {@link FandomUnavailableException} 을 던진다(없는 대상은 빈 값).
 */
public interface FandomQueryPort {

    Optional<TargetInfo> findTarget(Long targetId);

    Optional<MatchInfo> findMatch(Long matchId);

    record TargetInfo(Long id, String leagueCode, String name, String shortName, String colorPrimary,
                      String status) {
        public boolean isActive() {
            return "ACTIVE".equals(status);
        }
    }

    record MatchInfo(Long id, String leagueCode, Long homeTargetId, Long awayTargetId, String status) {
        public boolean involves(Long targetId) {
            return targetId != null && (targetId.equals(homeTargetId) || targetId.equals(awayTargetId));
        }

        public boolean isOpen() {
            return "SCHEDULED".equals(status) || "LIVE".equals(status);
        }
    }

    class FandomUnavailableException extends RuntimeException {
        public FandomUnavailableException(String message) {
            super(message);
        }
    }
}
