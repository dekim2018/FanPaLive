package com.livebroadcast.fandom.domain.fan;

import com.livebroadcast.common.error.DomainException;

public final class FanErrors {

    private FanErrors() {
    }

    public static DomainException primaryCooldown(long remainingDays) {
        return DomainException.conflict("PRIMARY_FAN_COOLDOWN",
                "주 응원팀은 바꾼 뒤 30일 동안 다시 바꿀 수 없습니다. " + remainingDays + "일 뒤에 바꿀 수 있습니다.");
    }

    public static DomainException notFan() {
        return DomainException.notFound("NOT_A_FAN", "팬으로 등록되어 있지 않습니다.");
    }

    public static DomainException primaryLeaveBlocked(long remainingDays) {
        return DomainException.conflict("PRIMARY_FAN_COOLDOWN",
                "주 응원팀은 정한 뒤 30일 동안 해제할 수 없습니다. " + remainingDays + "일 뒤에 해제할 수 있습니다.");
    }
}
