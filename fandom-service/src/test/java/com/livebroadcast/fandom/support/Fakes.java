package com.livebroadcast.fandom.support;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import com.livebroadcast.fandom.app.FandomProperties;
import com.livebroadcast.fandom.app.port.out.CatalogRepositoryPort;
import com.livebroadcast.fandom.app.port.out.DomainEventPublisherPort;
import com.livebroadcast.fandom.app.port.out.FanRepositoryPort;
import com.livebroadcast.fandom.app.port.out.FanTargetRepositoryPort;
import com.livebroadcast.fandom.app.port.out.RankingBoardPort;
import com.livebroadcast.fandom.app.port.out.TargetStatsPort;
import com.livebroadcast.fandom.domain.catalog.League;
import com.livebroadcast.fandom.domain.catalog.Sport;
import com.livebroadcast.fandom.domain.catalog.SportKind;
import com.livebroadcast.fandom.domain.fan.FanChange;
import com.livebroadcast.fandom.domain.fan.FanMembership;
import com.livebroadcast.fandom.domain.ranking.HeartDailyDelta;
import com.livebroadcast.fandom.domain.ranking.RankingScoreCodec;
import com.livebroadcast.fandom.domain.ranking.TargetActivity;
import com.livebroadcast.fandom.domain.ranking.TargetStats;
import com.livebroadcast.fandom.domain.target.FanTarget;
import com.livebroadcast.fandom.domain.target.TargetKind;
import com.livebroadcast.fandom.domain.target.TargetProfile;
import com.livebroadcast.fandom.domain.target.TargetStatus;

/** 서비스 테스트용 인메모리 가짜 구현 모음 */
public final class Fakes {

    public static final League KBO = new League(1L, "BASEBALL", "KBO", "KBO 리그", "대한민국", false, 1);
    public static final League EPL = new League(3L, "FOOTBALL", "EPL", "프리미어리그", "잉글랜드", true, 3);

    private Fakes() {
    }

    public static FandomProperties properties() {
        return new FandomProperties(5, 3, 10, 7, 2, 5, null, "03-01", "Asia/Seoul",
                new FandomProperties.Features(true, true, true));
    }

    public static FanTarget team(long id, League league, String name, String shortName) {
        return new FanTarget(id, league.id(), TargetKind.TEAM,
                new TargetProfile(name, null, shortName, "#131230", "#ED1C24", ""), TargetStatus.ACTIVE, true, null,
                Instant.parse("2026-03-01T00:00:00Z"));
    }

    public static class Catalog implements CatalogRepositoryPort {
        public final List<League> leagues = new ArrayList<>(List.of(KBO, EPL));

        @Override
        public List<Sport> findSports() {
            return List.of(new Sport("BASEBALL", "야구", SportKind.TEAM, 1),
                    new Sport("FOOTBALL", "축구", SportKind.TEAM, 2));
        }

        @Override
        public List<League> findLeagues() {
            return leagues;
        }

        @Override
        public Optional<League> findLeagueByCode(String code) {
            return leagues.stream().filter(l -> l.code().equals(code)).findFirst();
        }

        @Override
        public Optional<League> findLeagueById(Long id) {
            return leagues.stream().filter(l -> l.id().equals(id)).findFirst();
        }
    }

    public static class Targets implements FanTargetRepositoryPort {
        public final Map<Long, FanTarget> byId = new LinkedHashMap<>();
        final AtomicLong seq = new AtomicLong(100);

        public Targets add(FanTarget t) {
            byId.put(t.getId(), t);
            return this;
        }

        @Override
        public FanTarget save(FanTarget t) {
            FanTarget saved = t.getId() != null ? t : new FanTarget(seq.incrementAndGet(), t.getLeagueId(), t.getKind(),
                    t.getProfile(), t.getStatus(), t.isOfficial(), t.getCreatedBy(), t.getCreatedAt());
            byId.put(saved.getId(), saved);
            return saved;
        }

        @Override
        public Optional<FanTarget> findById(Long id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public List<FanTarget> findByIds(Collection<Long> ids) {
            return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
        }

        @Override
        public List<FanTarget> findActiveByLeague(Long leagueId) {
            return byId.values().stream()
                    .filter(t -> t.getLeagueId().equals(leagueId) && t.getStatus().isVisible()).toList();
        }

        @Override
        public List<FanTarget> findAllActive() {
            return byId.values().stream().filter(t -> t.getStatus().isVisible()).toList();
        }

        @Override
        public boolean existsByLeagueAndNormalizedName(Long leagueId, String normalizedName) {
            return byId.values().stream().anyMatch(t -> t.getLeagueId().equals(leagueId)
                    && t.getProfile().normalizedName().equals(normalizedName));
        }

        @Override
        public List<FanTarget> search(String keyword, int limit) {
            return byId.values().stream().filter(t -> t.getProfile().name().contains(keyword)).limit(limit).toList();
        }
    }

    public static class Fans implements FanRepositoryPort {
        public final Map<String, FanMembership> byKey = new LinkedHashMap<>();
        public final Map<String, Instant> lastPrimaryChange = new HashMap<>();

        static String key(Long u, Long t) {
            return u + ":" + t;
        }

        @Override
        public Optional<FanMembership> find(Long userId, Long targetId) {
            return Optional.ofNullable(byKey.get(key(userId, targetId)));
        }

        @Override
        public Optional<FanMembership> findPrimary(Long userId, Long leagueId) {
            return byKey.values().stream()
                    .filter(m -> m.userId().equals(userId) && m.leagueId().equals(leagueId) && m.primary()).findFirst();
        }

        @Override
        public FanMembership save(FanMembership m) {
            byKey.put(key(m.userId(), m.targetId()), m);
            return m;
        }

        @Override
        public void delete(Long userId, Long targetId) {
            byKey.remove(key(userId, targetId));
        }

        @Override
        public List<FanMembership> findByUser(Long userId) {
            return byKey.values().stream().filter(m -> m.userId().equals(userId)).toList();
        }

        @Override
        public List<FanMembership> findPrimaryByUsers(Collection<Long> userIds) {
            return byKey.values().stream().filter(m -> m.primary() && userIds.contains(m.userId())).toList();
        }

        @Override
        public Optional<Instant> findLastPrimaryChange(Long userId, Long leagueId) {
            return Optional.ofNullable(lastPrimaryChange.get(userId + ":" + leagueId));
        }

        @Override
        public void recordPrimaryChange(Long userId, Long leagueId, Instant at) {
            lastPrimaryChange.put(userId + ":" + leagueId, at);
        }

        @Override
        public void lockUser(Long userId) {
        }
    }

    public static class Stats implements TargetStatsPort {
        public final Map<Long, long[]> totals = new HashMap<>();
        public final Map<String, long[]> daily = new HashMap<>();
        public final Map<String, Instant> dailyLast = new HashMap<>();
        public final List<List<HeartDailyDelta>> heartCalls = new ArrayList<>();

        long[] total(Long id) {
            return totals.computeIfAbsent(id, k -> new long[4]);
        }

        long[] day(Long id, LocalDate d) {
            return daily.computeIfAbsent(id + "|" + d, k -> new long[3]);
        }

        @Override
        public void applyFanChanges(List<FanChange> changes, Instant at) {
            for (FanChange c : changes) {
                total(c.targetId())[0] += c.primaryDelta();
                total(c.targetId())[1] += c.followDelta();
            }
        }

        @Override
        public void addHearts(List<HeartDailyDelta> deltas) {
            heartCalls.add(deltas);
            for (HeartDailyDelta d : deltas) {
                day(d.targetId(), d.date())[0] += d.hearts();
                day(d.targetId(), d.date())[1] += d.heartPoints();
                total(d.targetId())[2] += d.hearts();
                dailyLast.merge(d.targetId() + "|" + d.date(), d.lastAt(), (a, b) -> a.isAfter(b) ? a : b);
            }
        }

        @Override
        public void addDonation(Long targetId, LocalDate date, long amount, Instant at) {
            day(targetId, date)[2] += amount;
            total(targetId)[3] += amount;
            dailyLast.merge(targetId + "|" + date, at, (a, b) -> a.isAfter(b) ? a : b);
        }

        @Override
        public long overwriteDailyDonation(Long targetId, LocalDate date, long amount) {
            long diff = amount - day(targetId, date)[2];
            day(targetId, date)[2] = amount;
            total(targetId)[3] += diff;
            return diff;
        }

        @Override
        public Map<Long, TargetStats> findStats(Collection<Long> targetIds) {
            Map<Long, TargetStats> result = new HashMap<>();
            for (Long id : targetIds) {
                long[] t = total(id);
                result.put(id, new TargetStats(id, t[0], t[1], t[2], t[3]));
            }
            return result;
        }

        @Override
        public Map<Long, TargetActivity> findActivities(Collection<Long> targetIds, LocalDate from,
                                                        LocalDate toExclusive) {
            Map<Long, TargetActivity> result = new HashMap<>();
            for (Long id : targetIds) {
                long hearts = 0;
                long points = 0;
                long donation = 0;
                Instant last = null;
                for (LocalDate d = from; d.isBefore(toExclusive); d = d.plusDays(1)) {
                    long[] v = daily.get(id + "|" + d);
                    if (v != null) {
                        hearts += v[0];
                        points += v[1];
                        donation += v[2];
                        Instant l = dailyLast.get(id + "|" + d);
                        if (l != null && (last == null || l.isAfter(last))) last = l;
                    }
                }
                result.put(id, new TargetActivity(id, hearts, points, donation, last));
            }
            return result;
        }

        @Override
        public Map<Long, Long> findDailyDonations(LocalDate date) {
            Map<Long, Long> result = new HashMap<>();
            daily.forEach((k, v) -> {
                String[] parts = k.split("\\|");
                if (parts[1].equals(date.toString()) && v[2] != 0) result.put(Long.valueOf(parts[0]), v[2]);
            });
            return result;
        }

        @Override
        public List<Long> findTopByFans(int limit) {
            return totals.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue()[0] + b.getValue()[1], a.getValue()[0] + a.getValue()[1]))
                    .limit(limit).map(Map.Entry::getKey).toList();
        }
    }

    /** Lua 스크립트와 같은 규칙으로 동작하는 인메모리 순위표 */
    public static class Board implements RankingBoardPort {
        public final Map<String, Map<Long, Double>> keys = new HashMap<>();
        public int applyCalls;

        @Override
        public void applyDelta(Long leagueId, String periodKey, Long targetId, long delta, long elapsed) {
            applyCalls++;
            Map<Long, Double> zset = keys.get(leagueId + ":" + periodKey);
            if (zset == null) {
                return; // 키가 없으면 다음 재계산 때 만든다(Lua 와 동일)
            }
            long current = zset.containsKey(targetId) ? RankingScoreCodec.decodeScore(zset.get(targetId)) : 0;
            zset.put(targetId, RankingScoreCodec.encode(Math.max(0, current + delta), elapsed));
        }

        @Override
        public Map<Long, Double> read(Long leagueId, String periodKey) {
            return new HashMap<>(keys.getOrDefault(leagueId + ":" + periodKey, Map.of()));
        }

        @Override
        public void replace(Long leagueId, String periodKey, Map<Long, Double> encodedScores, long ttlSeconds) {
            keys.put(leagueId + ":" + periodKey, new HashMap<>(encodedScores));
        }
    }

    public static class Events implements DomainEventPublisherPort {
        public final List<Object> published = new ArrayList<>();

        @Override
        public void publish(Object event) {
            published.add(event);
        }
    }
}
