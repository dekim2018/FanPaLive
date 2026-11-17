package com.livebroadcast.fandom.domain.match;

import com.livebroadcast.common.error.DomainException;

public final class MatchErrors {

    private MatchErrors() {
    }

    public static DomainException notFound() {
        return DomainException.notFound("MATCH_NOT_FOUND", "경기를 찾을 수 없습니다.");
    }

    public static DomainException sameTeams() {
        return DomainException.badRequest("SAME_TEAMS", "홈팀과 원정팀이 같을 수 없습니다.");
    }

    public static DomainException leagueMismatch() {
        return DomainException.badRequest("MATCH_LEAGUE_MISMATCH", "두 팀이 경기 리그에 속해 있어야 합니다.");
    }

    public static DomainException invalidTransition(MatchStatus from, MatchStatus to) {
        return DomainException.conflict("INVALID_MATCH_TRANSITION", from + " 상태에서 " + to + " 로 바꿀 수 없습니다.");
    }

    public static DomainException invalidScore() {
        return DomainException.badRequest("INVALID_SCORE", "점수는 0 이상이어야 합니다.");
    }

    public static DomainException scoreLocked() {
        return DomainException.conflict("MATCH_SCORE_LOCKED", "끝났거나 취소된 경기는 점수를 바꿀 수 없습니다. 승부 예측이 이미 정산되었습니다.");
    }

    public static DomainException closed() {
        return DomainException.conflict("MATCH_CLOSED", "끝났거나 취소된 경기에는 응원할 수 없습니다.");
    }
}
