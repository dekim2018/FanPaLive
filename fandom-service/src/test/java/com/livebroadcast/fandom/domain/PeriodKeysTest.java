package com.livebroadcast.fandom.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.livebroadcast.fandom.domain.ranking.PeriodKeys;
import com.livebroadcast.fandom.domain.ranking.RankingPeriod;

/** 기간 경계는 KST 기준. 경계 1ms 전후가 어느 기간에 들어가는지 고정한다. */
class PeriodKeysTest {

    private final PeriodKeys keys = new PeriodKeys(ZoneId.of("Asia/Seoul"), MonthDay.of(3, 1));

    @Test
    void 주간_경계는_KST_월요일_0시다() {
        // 2026-11-23(월) 00:00 KST = 2026-11-22T15:00:00Z
        Instant mondayStart = Instant.parse("2026-11-22T15:00:00Z");

        assertThat(keys.key(RankingPeriod.WEEKLY, mondayStart.minusMillis(1))).isEqualTo("W2026-47");
        assertThat(keys.key(RankingPeriod.WEEKLY, mondayStart)).isEqualTo("W2026-48");
    }

    @Test
    void UTC_로는_같은_날이어도_KST_날짜로_나눈다() {
        // 2026-11-30T15:00Z = 12월 1일 00:00 KST
        assertThat(keys.key(RankingPeriod.MONTHLY, Instant.parse("2026-11-30T14:59:59.999Z"))).isEqualTo("M2026-11");
        assertThat(keys.key(RankingPeriod.MONTHLY, Instant.parse("2026-11-30T15:00:00Z"))).isEqualTo("M2026-12");
    }

    @Test
    void 시즌은_3월_1일에_시작한다() {
        assertThat(keys.key(RankingPeriod.SEASON, LocalDate.of(2027, 2, 28))).isEqualTo("S2026");
        assertThat(keys.key(RankingPeriod.SEASON, LocalDate.of(2027, 3, 1))).isEqualTo("S2027");
        assertThat(keys.start(RankingPeriod.SEASON, LocalDate.of(2026, 11, 17))).isEqualTo(LocalDate.of(2026, 3, 1));
    }

    @Test
    void 주간_기간의_시작과_끝() {
        LocalDate tuesday = LocalDate.of(2026, 11, 17);

        assertThat(keys.start(RankingPeriod.WEEKLY, tuesday)).isEqualTo(LocalDate.of(2026, 11, 16));
        assertThat(keys.endExclusive(RankingPeriod.WEEKLY, tuesday)).isEqualTo(LocalDate.of(2026, 11, 23));
        assertThat(keys.previous(RankingPeriod.WEEKLY, tuesday)).isEqualTo(LocalDate.of(2026, 11, 15));
    }

    @Test
    void 시즌_시작부터_지난_초() {
        Instant seasonBegin = Instant.parse("2026-02-28T15:00:00Z");

        assertThat(keys.secondsSinceSeasonStart(seasonBegin)).isZero();
        assertThat(keys.secondsSinceSeasonStart(seasonBegin.plusSeconds(90))).isEqualTo(90L);
    }
}
