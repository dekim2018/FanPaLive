package com.livebroadcast.fandom.domain.level;

/**
 * 팬 등급. 하트, 후원, 승부 예측 적중으로 쌓은 팬 포인트로 정한다.
 * 포인트는 원장(fan_point_ledger)에만 쌓이고 돈으로 바꿀 수 없다.
 */
public enum FanLevel {
    ROOKIE("루키", 0),
    REGULAR("일반", 100),
    DEVOTED("진성", 1_000),
    LEGEND("영구결번", 5_000);

    private final String label;
    private final long minPoints;

    FanLevel(String label, long minPoints) {
        this.label = label;
        this.minPoints = minPoints;
    }

    public static FanLevel of(long points) {
        FanLevel result = ROOKIE;
        for (FanLevel level : values()) {
            if (points >= level.minPoints) {
                result = level;
            }
        }
        return result;
    }

    /** 다음 등급. 마지막 등급이면 null */
    public FanLevel next() {
        int i = ordinal() + 1;
        return i < values().length ? values()[i] : null;
    }

    public String label() {
        return label;
    }

    public long minPoints() {
        return minPoints;
    }
}
