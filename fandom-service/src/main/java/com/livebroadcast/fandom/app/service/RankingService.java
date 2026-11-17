package com.livebroadcast.fandom.app.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.livebroadcast.fandom.app.FandomProperties;
import com.livebroadcast.fandom.app.event.DonationApplied;
import com.livebroadcast.fandom.app.event.FanChangesApplied;
import com.livebroadcast.fandom.app.port.in.RankingMaintenanceUseCase;
import com.livebroadcast.fandom.app.port.in.RankingQueryUseCase;
import com.livebroadcast.fandom.app.port.out.CatalogRepositoryPort;
import com.livebroadcast.fandom.app.port.out.DonationTotalsPort;
import com.livebroadcast.fandom.app.port.out.FanTargetRepositoryPort;
import com.livebroadcast.fandom.app.port.out.HeartLogPort;
import com.livebroadcast.fandom.app.port.out.RankingBoardPort;
import com.livebroadcast.fandom.app.port.out.RankingSnapshotPort;
import com.livebroadcast.fandom.app.port.out.ReconcileLogPort;
import com.livebroadcast.fandom.app.port.out.TargetStatsPort;
import com.livebroadcast.fandom.domain.catalog.League;
import com.livebroadcast.fandom.domain.fan.FanChange;
import com.livebroadcast.fandom.domain.ranking.PeriodKeys;
import com.livebroadcast.fandom.domain.ranking.RankSnapshot;
import com.livebroadcast.fandom.domain.ranking.RankingPeriod;
import com.livebroadcast.fandom.domain.ranking.RankingRow;
import com.livebroadcast.fandom.domain.ranking.RankingScoreCodec;
import com.livebroadcast.fandom.domain.ranking.RankingSort;
import com.livebroadcast.fandom.domain.ranking.ScoreFormula;
import com.livebroadcast.fandom.domain.ranking.Supporter;
import com.livebroadcast.fandom.domain.ranking.TargetActivity;
import com.livebroadcast.fandom.domain.ranking.TargetStats;
import com.livebroadcast.fandom.domain.target.FanTarget;
import com.livebroadcast.fandom.domain.target.TargetErrors;

/**
 * 랭킹 조회와 유지보수.
 *
 * 원본은 DB(팬 등록, 하트 원장, 후원 원장)이고 Redis ZSET 은 실시간 순위표(캐시)다.
 * 실시간 순위표는 커밋된 변화만 반영하고(AFTER_COMMIT), 매일 DB 기준으로 다시 만들어 어긋난 값을 바로잡는다.
 * Redis 가 비어 있거나 죽어도 DB 에서 바로 계산해서 응답한다(조금 느릴 뿐 틀리지 않음).
 */
@Service
public class RankingService implements RankingQueryUseCase, RankingMaintenanceUseCase, RankingDeltaSink {

    private static final Logger log = LoggerFactory.getLogger(RankingService.class);

    private final CatalogRepositoryPort catalog;
    private final FanTargetRepositoryPort targets;
    private final TargetStatsPort stats;
    private final RankingBoardPort board;
    private final RankingSnapshotPort snapshots;
    private final HeartLogPort heartLogs;
    private final DonationTotalsPort donationTotals;
    private final ReconcileLogPort reconcileLogs;
    private final TargetLookup lookup;
    private final PeriodKeys keys;
    private final Clock clock;

    public RankingService(CatalogRepositoryPort catalog, FanTargetRepositoryPort targets, TargetStatsPort stats,
                          RankingBoardPort board, RankingSnapshotPort snapshots, HeartLogPort heartLogs,
                          DonationTotalsPort donationTotals, ReconcileLogPort reconcileLogs, TargetLookup lookup,
                          FandomProperties properties, Clock clock) {
        this.catalog = catalog;
        this.targets = targets;
        this.stats = stats;
        this.board = board;
        this.snapshots = snapshots;
        this.heartLogs = heartLogs;
        this.donationTotals = donationTotals;
        this.reconcileLogs = reconcileLogs;
        this.lookup = lookup;
        this.keys = properties.periodKeys();
        this.clock = clock;
    }

    // ===== 실시간 반영 =====

    /** 세 기간(주간, 월간, 시즌)의 순위표에 점수 변화를 반영한다. 실패해도 원본은 DB 에 있으므로 기록만 남긴다. */
    @Override
    public void applyDelta(Long leagueId, Long targetId, long delta, Instant at) {
        if (delta == 0) {
            return;
        }
        long elapsed = keys.secondsSinceSeasonStart(at);
        for (RankingPeriod period : RankingPeriod.values()) {
            try {
                board.applyDelta(leagueId, keys.key(period, at), targetId, delta, elapsed);
            } catch (RuntimeException e) {
                log.warn("실시간 순위표 반영 실패(다음 재계산 때 복구) leagueId={} targetId={} reason={}",
                        leagueId, targetId, e.getMessage());
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(FanChangesApplied event) {
        for (FanChange change : event.changes()) {
            applyDelta(event.leagueId(), change.targetId(), change.scoreDelta(), event.at());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(DonationApplied event) {
        applyDelta(event.leagueId(), event.targetId(), event.points(), event.approvedAt());
    }

    // ===== 조회 =====

    @Override
    @Transactional(readOnly = true)
    public RankingBoard ranking(String leagueCode, RankingPeriod period, RankingSort sort) {
        League league = catalog.findLeagueByCode(leagueCode).orElseThrow(TargetErrors::leagueNotFound);
        RankingPeriod p = period == null ? RankingPeriod.WEEKLY : period;
        RankingSort s = sort == null ? RankingSort.SCORE : sort;
        LocalDate today = keys.dateOf(clock.instant());
        List<RankingRow> rows = rows(league, p, today, s, true);
        return new RankingBoard(league, p, keys.key(p, today), keys.start(p, today), keys.endExclusive(p, today),
                rows);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OverallRow> overall(int limit) {
        List<Long> ids = stats.findTopByFans(Math.max(1, Math.min(limit, 50)));
        Map<Long, FanTarget> targetMap = new LinkedHashMap<>();
        targets.findByIds(ids).forEach(t -> targetMap.put(t.getId(), t));
        Map<Long, TargetStats> statMap = stats.findStats(ids);
        List<OverallRow> result = new ArrayList<>();
        int rank = 0;
        for (Long id : ids) {
            FanTarget t = targetMap.get(id);
            if (t == null || !t.getStatus().isVisible()) {
                continue;
            }
            TargetStats s = statMap.getOrDefault(id, TargetStats.empty(id));
            rank++;
            long fanScore = ScoreFormula.score(s.primaryFans(), s.followFans(), 0, 0);
            result.add(new OverallRow(rank, lookup.league(t.getLeagueId()), row(rank, t, s, TargetActivity.empty(id),
                    fanScore, null)));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer weeklyRank(Long targetId) {
        FanTarget target = lookup.target(targetId);
        League league = lookup.league(target.getLeagueId());
        return rows(league, RankingPeriod.WEEKLY, keys.dateOf(clock.instant()), RankingSort.SCORE, true).stream()
                .filter(r -> r.targetId().equals(targetId))
                .map(RankingRow::rank)
                .findFirst()
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RankSnapshot> history(Long targetId, int limit) {
        return snapshots.findHistory(targetId, RankingPeriod.WEEKLY, Math.max(1, Math.min(limit, 26)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Supporter> supporters(Long targetId, RankingPeriod period, int limit) {
        RankingPeriod p = period == null ? RankingPeriod.SEASON : period;
        LocalDate today = keys.dateOf(clock.instant());
        ZoneId zone = keys.zone();
        Instant from = keys.start(p, today).atStartOfDay(zone).toInstant();
        Instant to = keys.endExclusive(p, today).atStartOfDay(zone).toInstant();
        return heartLogs.findTopSupporters(targetId, from, to, Math.max(1, Math.min(limit, 50)));
    }

    // ===== 유지보수 =====

    @Override
    @Transactional(readOnly = true)
    public void rebuildAll() {
        LocalDate today = keys.dateOf(clock.instant());
        for (League league : catalog.findLeagues()) {
            for (RankingPeriod period : RankingPeriod.values()) {
                rebuild(league, period, today);
            }
        }
    }

    @Override
    @Transactional
    public int reconcileDonations(LocalDate date) {
        Map<Long, Long> expected = donationTotals.findTargetTotals(date);
        Map<Long, Long> recorded = stats.findDailyDonations(date);
        Set<Long> ids = new HashSet<>(expected.keySet());
        ids.addAll(recorded.keySet());
        Instant now = clock.instant();
        int fixed = 0;
        for (Long targetId : ids) {
            long want = expected.getOrDefault(targetId, 0L);
            long have = recorded.getOrDefault(targetId, 0L);
            if (want != have) {
                stats.overwriteDailyDonation(targetId, date, want);
                reconcileLogs.record(date, targetId, want, have, now);
                log.warn("후원 집계 대사 불일치 보정 date={} targetId={} 결제원본={} 집계={}", date, targetId, want, have);
                fixed++;
            }
        }
        return fixed;
    }

    @Override
    @Transactional
    public void snapshotPreviousWeek() {
        LocalDate lastWeek = keys.previous(RankingPeriod.WEEKLY, keys.dateOf(clock.instant()));
        String key = keys.key(RankingPeriod.WEEKLY, lastWeek);
        for (League league : catalog.findLeagues()) {
            List<RankingRow> rows = rows(league, RankingPeriod.WEEKLY, lastWeek, RankingSort.SCORE, false);
            if (!rows.isEmpty()) {
                snapshots.save(league.id(), RankingPeriod.WEEKLY, key, rows);
            }
        }
    }

    @Override
    @Transactional
    public Map<League, List<RankingRow>> snapshotSeason(LocalDate date) {
        String key = keys.key(RankingPeriod.SEASON, date);
        Map<League, List<RankingRow>> result = new LinkedHashMap<>();
        for (League league : catalog.findLeagues()) {
            List<RankingRow> rows = rows(league, RankingPeriod.SEASON, date, RankingSort.SCORE, false);
            if (!rows.isEmpty() && rows.get(0).score() > 0) {
                snapshots.save(league.id(), RankingPeriod.SEASON, key, rows);
                result.put(league, rows);
            }
        }
        return result;
    }

    // ===== 계산 =====

    private void rebuild(League league, RankingPeriod period, LocalDate date) {
        Map<Long, Double> encoded = new LinkedHashMap<>();
        for (Entry e : entries(league, period, date, false)) {
            encoded.put(e.target().getId(), e.encoded());
        }
        try {
            board.replace(league.id(), keys.key(period, date), encoded, ttlSeconds(period));
        } catch (RuntimeException e) {
            log.warn("순위표 재계산 실패 league={} period={} reason={}", league.code(), period, e.getMessage());
        }
    }

    private List<RankingRow> rows(League league, RankingPeriod period, LocalDate date, RankingSort sort,
                                  boolean useBoard) {
        List<Entry> entries = entries(league, period, date, useBoard);
        Comparator<Entry> byScore = Comparator.comparingDouble(Entry::encoded).reversed()
                .thenComparing(e -> e.target().getId());
        Comparator<Entry> order = switch (sort) {
            case SCORE -> byScore;
            case FANS -> Comparator.<Entry>comparingLong(e -> e.stats().totalFans()).reversed().thenComparing(byScore);
            case HEARTS -> Comparator.<Entry>comparingLong(e -> e.activity().hearts()).reversed().thenComparing(byScore);
            case DONATIONS -> Comparator.<Entry>comparingLong(e -> e.activity().donationAmount()).reversed()
                    .thenComparing(byScore);
        };
        List<Entry> sorted = entries.stream().sorted(order).toList();

        Map<Long, Integer> previous = Map.of();
        if (period == RankingPeriod.WEEKLY && sort == RankingSort.SCORE) {
            previous = snapshots.findRanks(league.id(), RankingPeriod.WEEKLY,
                    keys.key(RankingPeriod.WEEKLY, keys.previous(RankingPeriod.WEEKLY, date)));
        }
        List<RankingRow> rows = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            Entry e = sorted.get(i);
            int rank = i + 1;
            Integer before = previous.get(e.target().getId());
            rows.add(row(rank, e.target(), e.stats(), e.activity(), RankingScoreCodec.decodeScore(e.encoded()),
                    before == null ? null : before - rank));
        }
        return rows;
    }

    private List<Entry> entries(League league, RankingPeriod period, LocalDate date, boolean useBoard) {
        List<FanTarget> list = targets.findActiveByLeague(league.id());
        if (list.isEmpty()) {
            return List.of();
        }
        List<Long> ids = list.stream().map(FanTarget::getId).toList();
        Map<Long, TargetStats> statMap = stats.findStats(ids);
        Map<Long, TargetActivity> acts = stats.findActivities(ids, keys.start(period, date),
                keys.endExclusive(period, date));
        Map<Long, Double> live = useBoard ? readBoard(league, period, date) : Map.of();

        List<Entry> entries = new ArrayList<>();
        for (FanTarget t : list) {
            TargetStats s = statMap.getOrDefault(t.getId(), TargetStats.empty(t.getId()));
            TargetActivity a = acts.getOrDefault(t.getId(), TargetActivity.empty(t.getId()));
            Double encoded = live.get(t.getId());
            if (encoded == null) {
                long score = ScoreFormula.score(s.primaryFans(), s.followFans(), a.heartPoints(), a.donationAmount());
                long elapsed = a.lastActivityAt() == null
                        ? RankingScoreCodec.BASE - 1
                        : keys.secondsSinceSeasonStart(a.lastActivityAt());
                encoded = RankingScoreCodec.encode(score, elapsed);
            }
            entries.add(new Entry(t, s, a, encoded));
        }
        return entries;
    }

    /** 실시간 순위표를 읽는다. 비어 있으면(기간이 막 바뀜, Redis 재시작) 한 번 다시 만든 뒤 읽는다. */
    private Map<Long, Double> readBoard(League league, RankingPeriod period, LocalDate date) {
        String key = keys.key(period, date);
        try {
            Map<Long, Double> live = board.read(league.id(), key);
            if (live.isEmpty()) {
                rebuild(league, period, date);
                live = board.read(league.id(), key);
            }
            return live;
        } catch (RuntimeException e) {
            log.warn("실시간 순위표 조회 실패, DB 로 계산 league={} reason={}", league.code(), e.getMessage());
            return Map.of();
        }
    }

    private static RankingRow row(int rank, FanTarget t, TargetStats s, TargetActivity a, long score,
                                  Integer change) {
        return new RankingRow(rank, t.getId(), t.getProfile().name(), t.getProfile().shortName(),
                t.getProfile().colorPrimary(), t.getProfile().colorSecondary(), t.getKind().name(), score,
                s.primaryFans(), s.followFans(), a.hearts(), a.donationAmount(), change);
    }

    private static long ttlSeconds(RankingPeriod period) {
        return switch (period) {
            case WEEKLY -> 15L * 86_400;
            case MONTHLY -> 40L * 86_400;
            case SEASON -> 400L * 86_400;
        };
    }

    private record Entry(FanTarget target, TargetStats stats, TargetActivity activity, double encoded) {
    }
}
