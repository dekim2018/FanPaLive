package com.livebroadcast.fandom.domain.prediction;

/** 경기별 예측 분포 */
public record PredictionTally(long home, long away) {

    public long total() {
        return home + away;
    }

    public int homePercent() {
        return total() == 0 ? 50 : (int) Math.round(home * 100.0 / total());
    }
}
