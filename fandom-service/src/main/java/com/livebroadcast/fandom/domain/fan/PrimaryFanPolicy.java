package com.livebroadcast.fandom.domain.fan;

import java.time.Duration;
import java.time.Instant;

/**
 * 주 응원팀(리그당 1개) 변경 규칙. 랭킹을 위해 팀을 자주 갈아타는 것을 막으려고, 바꾼 뒤 일정 기간은 다시 바꿀 수 없다.
 * 처음 정할 때는 제한이 없다.
 */
public class PrimaryFanPolicy {

    private final Duration cooldown;

    public PrimaryFanPolicy(Duration cooldown) {
        this.cooldown = cooldown;
    }

    /**
     * @param lastChangedAt 이 리그에서 주 응원팀을 마지막으로 정한 시각. 처음이면 null
     */
    public void ensureCanChange(Instant lastChangedAt, Instant now) {
        long remaining = remainingDays(lastChangedAt, now);
        if (remaining > 0) {
            throw FanErrors.primaryCooldown(remaining);
        }
    }

    public void ensureCanLeave(Instant lastChangedAt, Instant now) {
        long remaining = remainingDays(lastChangedAt, now);
        if (remaining > 0) {
            throw FanErrors.primaryLeaveBlocked(remaining);
        }
    }

    /** 다시 바꿀 수 있을 때까지 남은 일수(올림). 바로 바꿀 수 있으면 0 */
    public long remainingDays(Instant lastChangedAt, Instant now) {
        if (lastChangedAt == null) {
            return 0;
        }
        Instant availableAt = lastChangedAt.plus(cooldown);
        if (!now.isBefore(availableAt)) {
            return 0;
        }
        long seconds = Duration.between(now, availableAt).toSeconds();
        return (seconds + 86_399) / 86_400;
    }

    public Instant availableAt(Instant lastChangedAt) {
        return lastChangedAt == null ? null : lastChangedAt.plus(cooldown);
    }
}
