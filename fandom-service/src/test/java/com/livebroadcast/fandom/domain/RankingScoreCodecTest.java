package com.livebroadcast.fandom.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.livebroadcast.fandom.domain.ranking.RankingScoreCodec;

class RankingScoreCodecTest {

    @Test
    void 점수가_같으면_먼저_도달한_쪽이_더_큰_값이다() {
        double early = RankingScoreCodec.encode(1500, 1_000);
        double late = RankingScoreCodec.encode(1500, 2_000);

        assertThat(early).isGreaterThan(late);
        assertThat(RankingScoreCodec.encode(1501, 9_999_999)).isGreaterThan(early);
    }

    @Test
    void 최대_점수와_최대_경과초에서도_정확히_되돌린다() {
        long max = RankingScoreCodec.MAX_SCORE;
        double encoded = RankingScoreCodec.encode(max, RankingScoreCodec.BASE - 1);

        assertThat(encoded).isLessThan(Math.pow(2, 53));
        assertThat(RankingScoreCodec.decodeScore(encoded)).isEqualTo(max);
        assertThat(RankingScoreCodec.decodeElapsed(encoded)).isEqualTo(RankingScoreCodec.BASE - 1);
    }

    @Test
    void 경과초가_0이어도_다음_점수로_넘어가지_않는다() {
        // 경과 0초면 하위 자리가 99,999,999 로 가장 크다. double 나눗셈이었다면 반올림으로 점수+1 이 될 수 있는 구간
        long score = 90_000_000L;
        double encoded = RankingScoreCodec.encode(score, 0);

        assertThat(RankingScoreCodec.decodeScore(encoded)).isEqualTo(score);
    }

    @Test
    void 음수와_범위_밖_값은_잘라낸다() {
        assertThat(RankingScoreCodec.decodeScore(RankingScoreCodec.encode(-50, 10))).isZero();
        assertThat(RankingScoreCodec.decodeScore(RankingScoreCodec.encode(Long.MAX_VALUE, 10)))
                .isEqualTo(RankingScoreCodec.MAX_SCORE);
    }
}
