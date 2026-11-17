package com.livebroadcast.fandom.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.livebroadcast.fandom.app.event.FanChangesApplied;
import com.livebroadcast.fandom.app.service.FanService;
import com.livebroadcast.fandom.app.service.TargetLookup;
import com.livebroadcast.fandom.domain.fan.FanChange;
import com.livebroadcast.fandom.support.Fakes;

/** 두산을 주 응원팀으로 정하고 바꾸는 규칙 */
class FanServiceTest {

    static final long DOOSAN = 9L;
    static final long LG = 1L;
    static final long USER = 77L;

    final Instant start = Instant.parse("2026-11-01T00:00:00Z");
    final Fakes.Fans fans = new Fakes.Fans();
    final Fakes.Stats stats = new Fakes.Stats();
    final Fakes.Events events = new Fakes.Events();
    final Fakes.Targets targets = new Fakes.Targets()
            .add(Fakes.team(DOOSAN, Fakes.KBO, "두산 베어스", "두산"))
            .add(Fakes.team(LG, Fakes.KBO, "LG 트윈스", "LG"));

    FanService service(Instant now) {
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        return new FanService(fans, targets, stats, new TargetLookup(targets, new Fakes.Catalog(), clock), events,
                Fakes.properties(), clock);
    }

    @Test
    void 주_응원팀을_정하면_주_응원팬이_1명_늘고_랭킹_반영_이벤트가_나간다() {
        service(start).join(USER, "곰돌이", DOOSAN, true);

        assertThat(stats.findStats(List.of(DOOSAN)).get(DOOSAN).primaryFans()).isEqualTo(1L);
        assertThat(events.published).singleElement()
                .satisfies(e -> assertThat(((FanChangesApplied) e).changes())
                        .containsExactly(new FanChange(DOOSAN, 1, 0)));
    }

    @Test
    void 주_응원팀을_바꾸면_예전_팀은_관심팬으로_내려간다() {
        service(start).join(USER, "곰돌이", LG, true);

        service(start.plus(Duration.ofDays(31))).join(USER, "곰돌이", DOOSAN, true);

        assertThat(fans.find(USER, LG).orElseThrow().primary()).isFalse();
        assertThat(fans.find(USER, DOOSAN).orElseThrow().primary()).isTrue();
        assertThat(stats.findStats(List.of(LG)).get(LG).followFans()).isEqualTo(1L);
        assertThat(stats.findStats(List.of(LG)).get(LG).primaryFans()).isZero();
    }

    @Test
    void 바꾼_지_30일이_안_됐으면_주_응원팀을_바꿀_수_없다() {
        service(start).join(USER, "곰돌이", LG, true);

        assertThatThrownBy(() -> service(start.plus(Duration.ofDays(5))).join(USER, "곰돌이", DOOSAN, true))
                .hasMessageContaining("25일");
        assertThat(fans.find(USER, DOOSAN)).isEmpty();
    }

    @Test
    void 주_응원팀을_다시_같은_팀으로_정하면_아무것도_바뀌지_않는다() {
        service(start).join(USER, "곰돌이", DOOSAN, true);
        events.published.clear();

        service(start.plus(Duration.ofDays(1))).join(USER, "곰돌이", DOOSAN, true);

        assertThat(events.published).isEmpty();
    }

    @Test
    void 관심팬은_언제든_해제할_수_있지만_주_응원팀은_30일_동안_해제할_수_없다() {
        service(start).join(USER, "곰돌이", LG, false);
        service(start).leave(USER, LG);
        assertThat(fans.find(USER, LG)).isEmpty();

        service(start).join(USER, "곰돌이", DOOSAN, true);
        assertThatThrownBy(() -> service(start.plus(Duration.ofDays(1))).leave(USER, DOOSAN))
                .hasMessageContaining("해제할 수 없습니다");
    }

    @Test
    void 배지는_주_응원팀만_보여준다() {
        service(start).join(USER, "곰돌이", DOOSAN, true);
        service(start).join(USER, "곰돌이", LG, false);

        var badges = service(start).badges(List.of(USER, 999L));

        assertThat(badges.get(USER)).extracting(b -> b.shortName()).containsExactly("두산");
        assertThat(badges.containsKey(999L)).isFalse();
    }
}
