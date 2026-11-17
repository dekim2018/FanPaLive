package com.livebroadcast.fandom.domain.prediction;

public enum PredictionResult {
    HIT,
    MISS,
    /** 무승부나 취소된 경기: 적중도 실패도 아님 */
    VOID
}
