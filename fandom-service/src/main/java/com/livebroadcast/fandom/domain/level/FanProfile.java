package com.livebroadcast.fandom.domain.level;

import java.util.ArrayList;
import java.util.List;

/**
 * 팬 포인트와 등급, 업적 배지.
 *
 * @param heartDays     하트를 준 날 수
 * @param donationCount 확정된 후원 횟수
 */
public record FanProfile(Long userId, String nickname, long points, int predictionHits, int predictionTotal,
                         int heartDays, int donationCount) {

    public static final int PREDICTOR_HITS = 5;
    public static final int HEART_KEEPER_DAYS = 7;

    public static FanProfile empty(Long userId) {
        return new FanProfile(userId, null, 0, 0, 0, 0, 0);
    }

    public FanLevel level() {
        return FanLevel.of(points);
    }

    /** 다음 등급까지 남은 포인트. 마지막 등급이면 0 */
    public long pointsToNext() {
        FanLevel next = level().next();
        return next == null ? 0 : next.minPoints() - points;
    }

    public List<Achievement> achievements() {
        List<Achievement> list = new ArrayList<>();
        if (predictionHits >= PREDICTOR_HITS) list.add(Achievement.PREDICTOR);
        if (heartDays >= HEART_KEEPER_DAYS) list.add(Achievement.HEART_KEEPER);
        if (donationCount > 0) list.add(Achievement.SUPPORTER);
        return list;
    }

    public enum Achievement {
        PREDICTOR("예언자", "승부 예측 5번 적중"),
        HEART_KEEPER("개근 응원단", "7일 이상 하트 응원"),
        SUPPORTER("서포터", "후원 또는 응원금 참여");

        private final String label;
        private final String description;

        Achievement(String label, String description) {
            this.label = label;
            this.description = description;
        }

        public String label() {
            return label;
        }

        public String description() {
            return description;
        }
    }
}
