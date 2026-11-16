package com.livebroadcast.fandom.domain.match;

/**
 * <pre>
 * SCHEDULED -> LIVE -> FINISHED
 * SCHEDULED -> CANCELED
 * </pre>
 */
public enum MatchStatus {
    SCHEDULED,
    LIVE,
    FINISHED,
    CANCELED;

    public boolean canTransitionTo(MatchStatus next) {
        return switch (this) {
            case SCHEDULED -> next == LIVE || next == CANCELED;
            case LIVE -> next == FINISHED;
            case FINISHED, CANCELED -> false;
        };
    }

    /** 응원 하트를 받을 수 있는 상태 */
    public boolean acceptsCheers() {
        return this == SCHEDULED || this == LIVE;
    }
}
