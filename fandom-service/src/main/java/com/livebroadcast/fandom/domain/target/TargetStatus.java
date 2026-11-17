package com.livebroadcast.fandom.domain.target;

public enum TargetStatus {
    /** 공개, 팬 등록과 하트 가능 */
    ACTIVE,
    /** 운영 중단(목록과 랭킹에서 숨김). 기존 기록은 남는다. */
    HIDDEN;

    public boolean isVisible() {
        return this == ACTIVE;
    }
}
