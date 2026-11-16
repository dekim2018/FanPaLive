package com.livebroadcast.fandom.domain.heart;

import com.livebroadcast.common.error.DomainException;

public final class HeartErrors {

    private HeartErrors() {
    }

    public static DomainException invalidAmount(int max) {
        return DomainException.badRequest("INVALID_HEART_AMOUNT", "하트는 한 번에 1~" + max + "개까지 줄 수 있습니다.");
    }

    public static DomainException dailyLimit(int dailyMax, int perTargetMax) {
        return DomainException.conflict("HEART_LIMIT_EXCEEDED",
                "오늘 줄 수 있는 하트를 모두 썼습니다. (하루 " + dailyMax + "개, 한 대상에 " + perTargetMax + "개)");
    }

    public static DomainException matchRequired() {
        return DomainException.badRequest("MATCH_REQUIRED", "경기 응원 하트는 경기를 지정해야 합니다.");
    }

    public static DomainException notInMatch() {
        return DomainException.badRequest("TARGET_NOT_IN_MATCH", "이 경기에 나오는 팀에만 응원 하트를 줄 수 있습니다.");
    }

    public static DomainException unavailable() {
        return new DomainException(com.livebroadcast.common.error.ErrorType.BAD_GATEWAY, "HEART_UNAVAILABLE",
                "지금은 하트를 줄 수 없습니다. 잠시 후 다시 시도해 주세요.");
    }
}
