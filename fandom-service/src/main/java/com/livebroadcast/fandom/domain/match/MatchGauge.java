package com.livebroadcast.fandom.domain.match;

/** 매치룸 응원 게이지(양 팀이 받은 하트 수) */
public record MatchGauge(long home, long away) {

    public static MatchGauge empty() {
        return new MatchGauge(0, 0);
    }

    /** 홈팀 비율(0~100). 둘 다 0 이면 50 */
    public int homePercent() {
        long total = home + away;
        return total == 0 ? 50 : (int) Math.round(home * 100.0 / total);
    }
}
