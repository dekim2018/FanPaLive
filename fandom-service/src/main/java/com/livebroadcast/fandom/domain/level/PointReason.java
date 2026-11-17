package com.livebroadcast.fandom.domain.level;

/**
 * 팬 포인트가 쌓이는 이유. (user_id, reason, ref_key) 가 유일해서 같은 근거로 두 번 쌓이지 않는다.
 */
public enum PointReason {
    /** 하루 동안 준 하트(새벽 배치). ref_key = 날짜 */
    HEART,
    /** 후원 확정. ref_key = 후원 이벤트 ID */
    DONATION,
    /** 승부 예측 적중. ref_key = 경기 ID */
    PREDICTION;

    public static final int POINTS_PER_HEART = 2;
    public static final long WON_PER_POINT = 100;
    public static final int PREDICTION_HIT_POINTS = 30;

    public static int donationPoints(long amount) {
        return (int) Math.max(1, amount / WON_PER_POINT);
    }
}
