package com.livebroadcast.fandom.app.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.livebroadcast.fandom.app.port.out.HeartLogPort;
import com.livebroadcast.fandom.app.port.out.MatchGaugePort;
import com.livebroadcast.fandom.app.port.out.TargetStatsPort;
import com.livebroadcast.fandom.domain.heart.HeartGrant;
import com.livebroadcast.fandom.domain.match.MatchSide;
import com.livebroadcast.fandom.domain.ranking.HeartDailyDelta;

/**
 * 하트 쓰기 병합(write coalescing).
 *
 * 문제: 홈런 같은 순간에는 한 팀의 카운터 하나(Redis 키, DB 행)에 초당 수천 건이 몰린다(핫 키).
 * 요청마다 UPDATE 하면 같은 행의 잠금을 두고 경합하고, 요청마다 ZSET 을 갱신하면 Redis 명령 수가 요청 수와 같아진다.
 *
 * 해결: 요청은 큐에만 넣고, 200ms 마다 모아서
 * 1) heart_logs 를 배치 INSERT 1번, 2) 대상/날짜별 통계 UPSERT 를 대상 수만큼, 3) 순위표 갱신을 대상 수만큼 한다.
 * 쓰기 횟수가 "하트 수"에서 "200ms 동안 하트를 받은 대상 수"로 줄어든다.
 *
 * 대가: 서버가 갑자기 죽으면 큐에 있던 최대 200ms 분량의 하트가 사라질 수 있다. 하트는 무료 재화이고 이 손실을 허용한다.
 * 돈(후원)은 이 경로를 쓰지 않는다.
 */
@Component
public class HeartAccumulator {

    private static final Logger log = LoggerFactory.getLogger(HeartAccumulator.class);
    /** 큐가 이 이상 쌓이면(DB 장애 등) 새 하트를 받지 않는다. 메모리를 지키기 위한 상한 */
    static final int MAX_QUEUE = 50_000;
    static final int MAX_BATCH = 5_000;
    static final int MAX_ATTEMPTS = 2;

    private final ConcurrentLinkedQueue<Pending> queue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger size = new AtomicInteger();
    private final AtomicLong flushedHearts = new AtomicLong();
    private final AtomicLong flushCount = new AtomicLong();
    private final AtomicLong droppedHearts = new AtomicLong();

    private final HeartLogPort heartLogs;
    private final TargetStatsPort stats;
    private final MatchGaugePort gauges;
    private final RankingDeltaSink ranking;
    private final TransactionTemplate tx;

    public HeartAccumulator(HeartLogPort heartLogs, TargetStatsPort stats, MatchGaugePort gauges,
                            RankingDeltaSink ranking, PlatformTransactionManager transactionManager) {
        this.heartLogs = heartLogs;
        this.stats = stats;
        this.gauges = gauges;
        this.ranking = ranking;
        this.tx = new TransactionTemplate(transactionManager);
    }

    public boolean enqueue(HeartGrant grant) {
        if (size.incrementAndGet() > MAX_QUEUE) {
            size.decrementAndGet();
            return false;
        }
        queue.add(new Pending(grant, 0));
        return true;
    }

    /** @return 이번에 반영한 하트 건수 */
    public int flush() {
        List<Pending> batch = new ArrayList<>();
        Pending p;
        while (batch.size() < MAX_BATCH && (p = queue.poll()) != null) {
            batch.add(p);
        }
        if (batch.isEmpty()) {
            return 0;
        }
        size.addAndGet(-batch.size());
        List<HeartGrant> grants = batch.stream().map(Pending::grant).toList();
        try {
            tx.execute(status -> {
                heartLogs.insertBatch(grants);
                stats.addHearts(dailyDeltas(grants));
                return null;
            });
        } catch (RuntimeException e) {
            retryOrDrop(batch, e);
            return 0;
        }
        applyRealtime(grants);
        flushedHearts.addAndGet(grants.size());
        flushCount.incrementAndGet();
        return grants.size();
    }

    public void flushAll() {
        while (flush() > 0) {
            // 큐가 빌 때까지 반복
        }
    }

    public int pendingCount() {
        return size.get();
    }

    public Stats stats() {
        return new Stats(flushedHearts.get(), flushCount.get(), droppedHearts.get(), size.get());
    }

    /** flushedHearts / flushCount 가 "쓰기 1번에 묶인 하트 수"다(병합 효과 측정용). */
    public record Stats(long flushedHearts, long flushCount, long droppedHearts, int pending) {
    }

    static List<HeartDailyDelta> dailyDeltas(List<HeartGrant> grants) {
        Map<String, long[]> sums = new LinkedHashMap<>();
        Map<String, HeartGrant> samples = new LinkedHashMap<>();
        Map<String, Instant> lastAt = new LinkedHashMap<>();
        for (HeartGrant g : grants) {
            String key = g.targetId() + "|" + g.givenDate();
            long[] s = sums.computeIfAbsent(key, k -> new long[3]);
            s[0] += g.countedHearts();
            s[1] += g.rankingPoints();
            s[2] += g.suspicious() ? g.amount() : 0;
            samples.putIfAbsent(key, g);
            lastAt.merge(key, g.createdAt(), (a, b) -> a.isAfter(b) ? a : b);
        }
        List<HeartDailyDelta> result = new ArrayList<>();
        sums.forEach((key, s) -> {
            HeartGrant sample = samples.get(key);
            result.add(new HeartDailyDelta(sample.targetId(), sample.givenDate(), s[0], s[1], s[2], lastAt.get(key)));
        });
        return result;
    }

    private void applyRealtime(List<HeartGrant> grants) {
        Map<Long, long[]> points = new LinkedHashMap<>();
        Map<Long, Long> leagues = new LinkedHashMap<>();
        Map<Long, Instant> lastAt = new LinkedHashMap<>();
        Map<String, Long> gaugeDeltas = new LinkedHashMap<>();
        for (HeartGrant g : grants) {
            points.computeIfAbsent(g.targetId(), k -> new long[1])[0] += g.rankingPoints();
            leagues.putIfAbsent(g.targetId(), g.leagueId());
            lastAt.merge(g.targetId(), g.createdAt(), (a, b) -> a.isAfter(b) ? a : b);
            if (g.matchId() != null && g.matchSide() != null && !g.suspicious()) {
                gaugeDeltas.merge(g.matchId() + "|" + g.matchSide().name(), (long) g.amount(), Long::sum);
            }
        }
        points.forEach((targetId, p) -> {
            if (p[0] > 0) {
                ranking.applyDelta(leagues.get(targetId), targetId, p[0], lastAt.get(targetId));
            }
        });
        gaugeDeltas.forEach((key, hearts) -> {
            String[] parts = key.split("\\|");
            try {
                gauges.add(Long.valueOf(parts[0]), MatchSide.valueOf(parts[1]), hearts);
            } catch (RuntimeException e) {
                log.warn("응원 게이지 반영 실패 matchId={} reason={}", parts[0], e.getMessage());
            }
        });
    }

    private void retryOrDrop(List<Pending> batch, RuntimeException e) {
        int retried = 0;
        for (Pending p : batch) {
            if (p.attempts() + 1 < MAX_ATTEMPTS) {
                if (size.incrementAndGet() <= MAX_QUEUE) {
                    queue.add(new Pending(p.grant(), p.attempts() + 1));
                    retried++;
                    continue;
                }
                size.decrementAndGet();
            }
            droppedHearts.addAndGet(p.grant().amount());
        }
        log.error("하트 반영 실패, 재시도 {}건 / 폐기 {}건 reason={}", retried, batch.size() - retried, e.getMessage());
    }

    private record Pending(HeartGrant grant, int attempts) {
    }
}
