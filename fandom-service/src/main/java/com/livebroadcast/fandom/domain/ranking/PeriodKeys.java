package com.livebroadcast.fandom.domain.ranking;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;

/**
 * 랭킹 기간 계산. 모든 경계는 KST 기준이다(서버 시간대와 무관하게 Clock 과 zone 을 받아서 계산).
 * 경계 시각 1ms 전후로 어느 기간에 들어가는지가 바뀌므로 테스트로 고정해 둔다.
 */
public class PeriodKeys {

    private final ZoneId zone;
    private final MonthDay seasonStart;

    public PeriodKeys(ZoneId zone, MonthDay seasonStart) {
        this.zone = zone;
        this.seasonStart = seasonStart;
    }

    public ZoneId zone() {
        return zone;
    }

    public LocalDate dateOf(Instant instant) {
        return instant.atZone(zone).toLocalDate();
    }

    public String key(RankingPeriod period, Instant instant) {
        return key(period, dateOf(instant));
    }

    public String key(RankingPeriod period, LocalDate date) {
        return switch (period) {
            case WEEKLY -> "W" + date.get(IsoFields.WEEK_BASED_YEAR) + "-"
                    + String.format("%02d", date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            case MONTHLY -> "M" + date.getYear() + "-" + String.format("%02d", date.getMonthValue());
            case SEASON -> "S" + seasonYear(date);
        };
    }

    /** 기간의 시작일(포함) */
    public LocalDate start(RankingPeriod period, LocalDate date) {
        return switch (period) {
            case WEEKLY -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTHLY -> date.withDayOfMonth(1);
            case SEASON -> seasonStart.atYear(seasonYear(date));
        };
    }

    /** 기간의 끝(다음 기간 시작일, 미포함) */
    public LocalDate endExclusive(RankingPeriod period, LocalDate date) {
        LocalDate start = start(period, date);
        return switch (period) {
            case WEEKLY -> start.plusWeeks(1);
            case MONTHLY -> start.plusMonths(1);
            case SEASON -> start.plusYears(1);
        };
    }

    /** 바로 이전 기간에 속한 날짜 */
    public LocalDate previous(RankingPeriod period, LocalDate date) {
        return start(period, date).minusDays(1);
    }

    /** 시즌 시작부터 지난 초. 동점일 때 먼저 도달한 쪽을 앞에 두는 데 쓴다. */
    public long secondsSinceSeasonStart(Instant instant) {
        LocalDate date = dateOf(instant);
        Instant seasonBegin = start(RankingPeriod.SEASON, date).atStartOfDay(zone).toInstant();
        return Math.max(0, instant.getEpochSecond() - seasonBegin.getEpochSecond());
    }

    private int seasonYear(LocalDate date) {
        LocalDate thisYearStart = seasonStart.atYear(date.getYear());
        return date.isBefore(thisYearStart) ? date.getYear() - 1 : date.getYear();
    }
}
