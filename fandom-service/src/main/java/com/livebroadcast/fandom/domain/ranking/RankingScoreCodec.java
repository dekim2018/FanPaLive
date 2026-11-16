package com.livebroadcast.fandom.domain.ranking;

/**
 * Redis ZSET 점수 하나에 (응원 점수, 먼저 도달한 순서)를 함께 담는다.
 *
 * <pre>
 * encoded = score * BASE + (BASE - 1 - 시즌 시작 후 지난 초)
 * </pre>
 *
 * ZSET 점수는 double 이라 정수를 정확히 담을 수 있는 범위가 2^53 까지다.
 * BASE = 10^8 이면 시즌 경과 초는 1억 초(약 3.17년)까지, 점수는 2^53 / 10^8 = 약 9천만점까지 정확하다.
 * 같은 점수면 더 일찍 그 점수에 도달한 대상의 encoded 값이 커서 앞 순위가 된다.
 *
 * 디코딩은 double 나눗셈 대신 long 정수 연산으로 한다. 점수가 크면 v / 10^8 의 반올림 오차로
 * 다음 정수가 되어 버릴 수 있기 때문이다(Lua 쪽도 math.fmod 로 같은 문제를 피한다).
 */
public final class RankingScoreCodec {

    public static final long BASE = 100_000_000L;
    /** 2^53 - 1 보다 작은 encoded 값을 보장하는 최대 점수 */
    public static final long MAX_SCORE = ((1L << 53) - 1) / BASE - 1;

    private RankingScoreCodec() {
    }

    public static double encode(long score, long secondsSinceSeasonStart) {
        long safeScore = Math.max(0, Math.min(score, MAX_SCORE));
        long elapsed = Math.max(0, Math.min(secondsSinceSeasonStart, BASE - 1));
        return (double) (safeScore * BASE + (BASE - 1 - elapsed));
    }

    public static long decodeScore(double encoded) {
        long value = (long) encoded;
        return value / BASE;
    }

    public static long decodeElapsed(double encoded) {
        long value = (long) encoded;
        return BASE - 1 - (value % BASE);
    }
}
