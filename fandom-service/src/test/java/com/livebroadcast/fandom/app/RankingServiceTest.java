package com.livebroadcast.fandom.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.livebroadcast.fandom.app.port.out.RankingSnapshotPort;
import com.livebroadcast.fandom.app.service.RankingService;
import com.livebroadcast.fandom.app.service.TargetLookup;
import com.livebroadcast.fandom.domain.fan.FanChange;
import com.livebroadcast.fandom.domain.ranking.RankSnapshot;
import com.livebroadcast.fandom.domain.ranking.RankingPeriod;
import com.livebroadcast.fandom.domain.ranking.RankingRow;
import com.livebroadcast.fandom.domain.ranking.RankingSort;
import com.livebroadcast.fandom.domain.ranking.HeartDailyDelta;
import com.livebroadcast.fandom.support.Fakes;

class RankingServiceTest {

    static final long DOOSAN = 9L;
    static final long LG = 1L;
    static final long KIA = 8L;

    final Instant now = Instant.parse("2026-11-17T03:00:00Z"); // 화요일 12:00 KST
    final LocalDate today = LocalDate.of(2026, 11, 17);
    final Fakes.Stats stats = new Fakes.Stats();
    final Fakes.Board board = new Fakes.Board();
    final Fakes.Catalog catalog = new Fakes.Catalog();
    final Fakes.Targets targets = new Fakes.Targets()
            .add(Fakes.team(DOOSAN, Fakes.KBO, "두산 베어스", "두산"))
            .add(Fakes.team(LG, Fakes.KBO, "LG 트윈스", "LG"))
            .add(Fakes.team(KIA, Fakes.KBO, "KIA 타이거즈", "KIA"));
    final Map<String, Map<Long, Integer>> snapshotRanks = new HashMap<>();
    final List<String> reconcileLog = new ArrayList<>();
    final Map<Long, Long> paymentTotals = new HashMap<>();

    final RankingSnapshotPort snapshots = new RankingSnapshotPort() {
        @Override
        public void save(Long leagueId, RankingPeriod period, String periodKey, List<RankingRow> rows) {
            Map<Long, Integer> ranks = new HashMap<>();
            rows.forEach(r -> ranks.put(r.targetId(), r.rank()));
            snapshotRanks.put(periodKey, ranks);
        }

        @Override
        public Map<Long, Integer> findRanks(Long leagueId, RankingPeriod period, String periodKey) {
            return snapshotRanks.getOrDefault(periodKey, Map.of());
        }

        @Override
        public List<RankSnapshot> findHistory(Long targetId, RankingPeriod period, int limit) {
            return List.of();
        }
    };

    RankingService service(Instant at) {
        Clock clock = Clock.fixed(at, ZoneOffset.UTC);
        return new RankingService(catalog, targets, stats, board, snapshots, null, date -> paymentTotals,
                (date, targetId, expected, actual, t) -> reconcileLog.add(targetId + ":" + expected + "/" + actual),
                new TargetLookup(targets, catalog, clock), Fakes.properties(), clock);
    }

    @Test
    void 응원_점수는_주_응원팬_100_관심팬_20_하트_점수_후원_100원당_1점() {
        stats.applyFanChanges(List.of(new FanChange(DOOSAN, 2, 1)), now);
        stats.addHearts(List.of(new HeartDailyDelta(DOOSAN, today, 3, 30, 0, now)));
        stats.addDonation(DOOSAN, today, 5000, now);

        RankingRow row = service(now).ranking("KBO", RankingPeriod.WEEKLY, RankingSort.SCORE).rows().get(0);

        assertThat(row.targetId()).isEqualTo(DOOSAN);
        assertThat(row.score()).isEqualTo(2 * 100 + 20 + 30 + 50L);
    }

    @Test
    void 점수가_같으면_먼저_그_점수에_도달한_팀이_앞선다() {
        stats.addHearts(List.of(new HeartDailyDelta(LG, today, 1, 10, 0, now.minusSeconds(60))));
        stats.addHearts(List.of(new HeartDailyDelta(DOOSAN, today, 1, 10, 0, now)));

        List<RankingRow> rows = service(now).ranking("KBO", RankingPeriod.WEEKLY, RankingSort.SCORE).rows();

        assertThat(rows).extracting(RankingRow::targetId).containsExactly(LG, DOOSAN, KIA);
    }

    @Test
    void 실시간_반영은_커밋된_변화만_순위표에_더하고_재계산하면_DB_와_같아진다() {
        RankingService service = service(now);
        service.ranking("KBO", RankingPeriod.WEEKLY, RankingSort.SCORE); // 비어 있으면 만들어진다
        stats.addHearts(List.of(new HeartDailyDelta(KIA, today, 5, 50, 0, now)));
        service.applyDelta(Fakes.KBO.id(), KIA, 50, now);

        assertThat(service.ranking("KBO", RankingPeriod.WEEKLY, RankingSort.SCORE).rows().get(0).targetId())
                .isEqualTo(KIA);

        // Redis 에만 잘못 더해진 값(예: 롤백된 트랜잭션)이 있어도 재계산하면 DB 기준으로 돌아온다.
        service.applyDelta(Fakes.KBO.id(), LG, 1000, now);
        service.rebuildAll();

        assertThat(service.ranking("KBO", RankingPeriod.WEEKLY, RankingSort.SCORE).rows().get(0).targetId())
                .isEqualTo(KIA);
    }

    @Test
    void 지난주_스냅샷과_비교해서_순위_변동을_보여준다() {
        stats.addHearts(List.of(new HeartDailyDelta(KIA, LocalDate.of(2026, 11, 12), 9, 90, 0, now)));
        service(now).snapshotPreviousWeek();
        stats.addHearts(List.of(new HeartDailyDelta(DOOSAN, today, 20, 200, 0, now)));

        List<RankingRow> rows = service(now).ranking("KBO", RankingPeriod.WEEKLY, RankingSort.SCORE).rows();

        assertThat(rows.get(0).targetId()).isEqualTo(DOOSAN);
        assertThat(rows.get(0).rankChange()).isEqualTo(-1 + 3);
    }

    @Test
    void 결제_원본과_후원_집계가_다르면_원본_기준으로_맞춘다() {
        stats.addDonation(DOOSAN, today, 5000, now);
        paymentTotals.put(DOOSAN, 15000L);
        paymentTotals.put(LG, 1000L);

        int fixed = service(now).reconcileDonations(today);

        assertThat(fixed).isEqualTo(2);
        assertThat(stats.findDailyDonations(today).get(DOOSAN)).isEqualTo(15000L);
        assertThat(stats.findStats(List.of(DOOSAN)).get(DOOSAN).donationTotal()).isEqualTo(15000L);
        assertThat(reconcileLog).containsExactlyInAnyOrder("9:15000/5000", "1:1000/0");
    }

    @Test
    void 팬_수_정렬() {
        stats.applyFanChanges(List.of(new FanChange(LG, 0, 3), new FanChange(DOOSAN, 1, 0)), now);

        List<RankingRow> rows = service(now).ranking("KBO", RankingPeriod.SEASON, RankingSort.FANS).rows();

        assertThat(rows).extracting(RankingRow::targetId).containsExactly(LG, DOOSAN, KIA);
    }
}
