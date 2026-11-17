package com.livebroadcast.fandom.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.livebroadcast.fandom.domain.level.FanLevel;
import com.livebroadcast.fandom.domain.level.FanProfile;
import com.livebroadcast.fandom.domain.level.FanProfile.Achievement;
import com.livebroadcast.fandom.domain.level.PointReason;
import com.livebroadcast.fandom.domain.schedule.ScheduleEntry;
import com.livebroadcast.fandom.domain.schedule.ScheduleTextParser;

class FanLevelAndScheduleTest {

    @Test
    void 포인트_경계값에서_등급이_바뀐다() {
        assertThat(FanLevel.of(0)).isEqualTo(FanLevel.ROOKIE);
        assertThat(FanLevel.of(99)).isEqualTo(FanLevel.ROOKIE);
        assertThat(FanLevel.of(100)).isEqualTo(FanLevel.REGULAR);
        assertThat(FanLevel.of(1000)).isEqualTo(FanLevel.DEVOTED);
        assertThat(FanLevel.of(5000)).isEqualTo(FanLevel.LEGEND);
        assertThat(FanLevel.LEGEND.next()).isNull();
    }

    @Test
    void 다음_등급까지_남은_포인트와_업적을_계산한다() {
        FanProfile p = new FanProfile(1L, "곰돌", 950, 5, 8, 7, 0);

        assertThat(p.pointsToNext()).isEqualTo(50);
        assertThat(p.achievements()).containsExactly(Achievement.PREDICTOR, Achievement.HEART_KEEPER);
    }

    @Test
    void 후원_포인트는_100원당_1점이고_최소_1점이다() {
        assertThat(PointReason.donationPoints(10_000)).isEqualTo(100);
        assertThat(PointReason.donationPoints(50)).isEqualTo(1);
    }

    @Test
    void 일정_CSV와_탭_구분_글을_읽고_머리줄과_주석은_건너뛴다() {
        String text = """
                날짜,시각,홈팀,원정팀,구장
                # 개막 2연전
                2026-04-01,18:30,두산 베어스,LG 트윈스,잠실
                2026.04.02\t9:30\t두산\tLG\t잠실
                """;

        List<ScheduleEntry> entries = ScheduleTextParser.parse(text);

        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).startsAt()).isEqualTo(LocalDateTime.of(2026, 4, 1, 18, 30));
        assertThat(entries.get(0).homeName()).isEqualTo("두산 베어스");
        assertThat(entries.get(1).startsAt()).isEqualTo(LocalDateTime.of(2026, 4, 2, 9, 30));
        assertThat(entries.get(1).venue()).isEqualTo("잠실");
    }

    @Test
    void 잘못된_줄은_줄번호와_이유를_남긴다() {
        List<ScheduleEntry> entries = ScheduleTextParser.parse("2026-13-01,18:30,두산,LG\n2026-04-01,18:30,두산");

        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).isValid()).isFalse();
        assertThat(entries.get(0).error()).contains("날짜");
        assertThat(entries.get(1).line()).isEqualTo(2);
        assertThat(entries.get(1).error()).contains("네 칸");
    }
}
