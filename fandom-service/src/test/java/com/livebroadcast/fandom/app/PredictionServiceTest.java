package com.livebroadcast.fandom.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.livebroadcast.fandom.app.port.in.PredictionUseCase.PredictionView;
import com.livebroadcast.fandom.app.service.PredictionService;
import com.livebroadcast.fandom.domain.match.Match;
import com.livebroadcast.fandom.domain.match.MatchSide;
import com.livebroadcast.fandom.domain.match.MatchStatus;
import com.livebroadcast.fandom.domain.prediction.PredictionResult;
import com.livebroadcast.fandom.support.ExtrasFakes;
import com.livebroadcast.fandom.support.Fakes;

class PredictionServiceTest {

    final Instant now = Instant.parse("2026-11-17T09:00:00Z");
    final Instant startsAt = now.plusSeconds(3600);
    final ExtrasFakes.Matches matches = new ExtrasFakes.Matches();
    final ExtrasFakes.Predictions predictions = new ExtrasFakes.Predictions();
    final ExtrasFakes.Ledger ledger = new ExtrasFakes.Ledger();
    final ExtrasFakes.Notifications notifications = new ExtrasFakes.Notifications();

    PredictionService service(Instant at) {
        return new PredictionService(predictions, matches, ledger, notifications,
                new FeatureGuard(Fakes.properties()), Clock.fixed(at, ZoneOffset.UTC));
    }

    Match match() {
        return matches.save(Match.schedule(1L, 9L, 3L, startsAt, "잠실", 1L, now));
    }

    @Test
    void 경기_시작_전에는_고르고_바꿀_수_있고_분포가_보인다() {
        Match m = match();
        service(now).predict(1L, "곰돌", m.getId(), MatchSide.AWAY);
        service(now).predict(1L, "곰돌", m.getId(), MatchSide.HOME);
        PredictionView v = service(now).predict(2L, "잠실", m.getId(), MatchSide.AWAY);

        assertThat(v.tally().home()).isEqualTo(1);
        assertThat(v.tally().away()).isEqualTo(1);
        assertThat(v.mine().pick()).isEqualTo(MatchSide.AWAY);
        assertThat(v.open()).isTrue();
    }

    @Test
    void 경기가_시작되면_예측할_수_없다() {
        Match m = match();

        assertThatThrownBy(() -> service(startsAt).predict(1L, "곰돌", m.getId(), MatchSide.HOME))
                .hasMessageContaining("예측할 수 없습니다");
    }

    @Test
    void 정산하면_적중자만_포인트를_받고_다시_정산해도_늘지_않는다() {
        Match m = match();
        service(now).predict(1L, "곰돌", m.getId(), MatchSide.HOME);
        service(now).predict(2L, "잠실", m.getId(), MatchSide.AWAY);
        m.changeStatus(MatchStatus.LIVE, startsAt);
        m.updateScore(5, 3, startsAt);
        m.changeStatus(MatchStatus.FINISHED, startsAt);

        int settled = service(startsAt).settle(m.getId());
        int again = service(startsAt).settle(m.getId());

        assertThat(settled).isEqualTo(2);
        assertThat(again).isZero();
        assertThat(ledger.points).containsEntry(1L, 30L).doesNotContainKey(2L);
        assertThat(predictions.find(m.getId(), 2L).orElseThrow().result()).isEqualTo(PredictionResult.MISS);
        assertThat(notifications.rows).hasSize(2);
    }

    @Test
    void 취소된_경기는_모두_무효이고_포인트가_없다() {
        Match m = match();
        service(now).predict(1L, "곰돌", m.getId(), MatchSide.HOME);
        m.changeStatus(MatchStatus.CANCELED, now);

        service(now).settle(m.getId());

        assertThat(predictions.find(m.getId(), 1L).orElseThrow().result()).isEqualTo(PredictionResult.VOID);
        assertThat(ledger.points).isEmpty();
        assertThat(notifications.rows).isEmpty();
    }
}
