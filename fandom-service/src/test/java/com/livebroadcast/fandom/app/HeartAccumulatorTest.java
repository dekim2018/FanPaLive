package com.livebroadcast.fandom.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import com.livebroadcast.fandom.support.NoopTransactionManager;

import com.livebroadcast.fandom.app.port.out.HeartLogPort;
import com.livebroadcast.fandom.app.port.out.MatchGaugePort;
import com.livebroadcast.fandom.app.service.HeartAccumulator;
import com.livebroadcast.fandom.domain.heart.HeartGrant;
import com.livebroadcast.fandom.domain.heart.HeartSource;
import com.livebroadcast.fandom.domain.match.MatchGauge;
import com.livebroadcast.fandom.domain.match.MatchSide;
import com.livebroadcast.fandom.domain.ranking.Supporter;
import com.livebroadcast.fandom.support.Fakes;

/** 홈런 순간처럼 한 팀에 하트가 몰려도 DB/Redis 쓰기는 "대상 수"만큼만 일어나는지 */
class HeartAccumulatorTest {

    final Instant now = Instant.parse("2026-11-17T10:00:00Z");
    final LocalDate today = LocalDate.of(2026, 11, 17);
    final Fakes.Stats stats = new Fakes.Stats();
    final List<String> rankingCalls = new ArrayList<>();
    final List<Long> gaugeAdds = new ArrayList<>();
    final FakeLogs logs = new FakeLogs();

    final MatchGaugePort gauges = new MatchGaugePort() {
        @Override
        public void add(Long matchId, MatchSide side, long hearts) {
            gaugeAdds.add(hearts);
        }

        @Override
        public MatchGauge read(Long matchId) {
            return MatchGauge.empty();
        }
    };

    final HeartAccumulator accumulator = new HeartAccumulator(logs, stats, gauges,
            (leagueId, targetId, delta, at) -> rankingCalls.add(targetId + ":" + delta),
            new NoopTransactionManager());

    HeartGrant heart(long user, long target, boolean suspicious, Long matchId) {
        return new HeartGrant(user, "u" + user, target, 1L, matchId, matchId == null ? null : MatchSide.HOME, 1, 10,
                suspicious, matchId == null ? HeartSource.TARGET_PAGE : HeartSource.MATCH, today, now);
    }

    @Test
    void 천_개의_하트를_한_번의_배치_INSERT_와_대상별_한_번의_갱신으로_반영한다() {
        for (int i = 0; i < 1000; i++) {
            accumulator.enqueue(heart(i, i % 10 == 0 ? 3L : 9L, false, null));
        }

        int flushed = accumulator.flush();

        assertThat(flushed).isEqualTo(1000);
        assertThat(logs.batches).hasSize(1);
        assertThat(stats.heartCalls).singleElement().satisfies(deltas -> assertThat(deltas).hasSize(2));
        assertThat(rankingCalls).containsExactlyInAnyOrder("9:9000", "3:1000");
        assertThat(accumulator.pendingCount()).isZero();
    }

    @Test
    void 의심_하트는_기록하지만_랭킹과_게이지에는_넣지_않는다() {
        accumulator.enqueue(heart(1, 9L, true, 5L));
        accumulator.enqueue(heart(2, 9L, false, 5L));

        accumulator.flush();

        assertThat(logs.batches.get(0)).hasSize(2);
        assertThat(rankingCalls).containsExactly("9:10");
        assertThat(gaugeAdds).containsExactly(1L);
    }

    @Test
    void DB_반영이_실패하면_한_번_다시_시도하고_그래도_실패하면_버린다() {
        logs.failTimes = 5;
        accumulator.enqueue(heart(1, 9L, false, null));

        accumulator.flush();
        assertThat(accumulator.pendingCount()).isEqualTo(1);
        accumulator.flush();

        assertThat(accumulator.pendingCount()).isZero();
        assertThat(accumulator.stats().droppedHearts()).isEqualTo(1L);
        assertThat(rankingCalls).isEmpty();
    }

    static class FakeLogs implements HeartLogPort {
        final List<List<HeartGrant>> batches = new ArrayList<>();
        int failTimes;

        @Override
        public void insertBatch(List<HeartGrant> grants) {
            if (failTimes-- > 0) {
                throw new IllegalStateException("db down");
            }
            batches.add(grants);
        }

        @Override
        public List<Supporter> findTopSupporters(Long targetId, Instant from, Instant to, int limit) {
            return List.of();
        }
    }
}
