package com.livebroadcast.fandom.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import com.livebroadcast.fandom.support.NoopTransactionManager;

import com.livebroadcast.fandom.app.port.in.HeartUseCase.GiveCommand;
import com.livebroadcast.fandom.app.port.out.AbuseSignalPort;
import com.livebroadcast.fandom.app.port.out.HeartLimiterPort;
import com.livebroadcast.fandom.app.port.out.HeartLogPort;
import com.livebroadcast.fandom.app.port.out.MatchGaugePort;
import com.livebroadcast.fandom.app.port.out.MatchRepositoryPort;
import com.livebroadcast.fandom.app.service.HeartAccumulator;
import com.livebroadcast.fandom.app.service.HeartService;
import com.livebroadcast.fandom.app.service.MatchService;
import com.livebroadcast.fandom.app.service.TargetLookup;
import com.livebroadcast.fandom.domain.heart.AbuseSignals;
import com.livebroadcast.fandom.domain.heart.HeartGrant;
import com.livebroadcast.fandom.domain.heart.HeartPolicy;
import com.livebroadcast.fandom.domain.heart.HeartSource;
import com.livebroadcast.fandom.domain.heart.LimitDecision;
import com.livebroadcast.fandom.domain.match.Match;
import com.livebroadcast.fandom.domain.match.MatchGauge;
import com.livebroadcast.fandom.domain.match.MatchSide;
import com.livebroadcast.fandom.domain.ranking.Supporter;
import com.livebroadcast.fandom.support.Fakes;

class HeartServiceTest {

    final Instant now = Instant.parse("2026-11-17T10:00:00Z");
    final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    final Fakes.Targets targets = new Fakes.Targets()
            .add(Fakes.team(9L, Fakes.KBO, "두산 베어스", "두산"))
            .add(Fakes.team(1L, Fakes.KBO, "LG 트윈스", "LG"))
            .add(Fakes.team(8L, Fakes.KBO, "KIA 타이거즈", "KIA"));
    final Fakes.Catalog catalog = new Fakes.Catalog();
    final TargetLookup lookup = new TargetLookup(targets, catalog, clock);
    final FeatureGuard features = new FeatureGuard(Fakes.properties());

    /** Lua 스크립트와 같은 규칙의 인메모리 한도 */
    final Map<String, Integer> used = new HashMap<>();
    final HeartLimiterPort limiter = new HeartLimiterPort() {
        @Override
        public LimitDecision tryConsume(Long userId, Long targetId, LocalDate date, int amount, HeartPolicy p) {
            int total = used.getOrDefault(userId + "|" + date, 0);
            int perTarget = used.getOrDefault(userId + "|" + targetId + "|" + date, 0);
            if (total + amount > p.dailyMax() || perTarget + amount > p.perTargetMax()) {
                return new LimitDecision(false, p.dailyMax() - total, p.perTargetMax() - perTarget);
            }
            used.put(userId + "|" + date, total + amount);
            used.put(userId + "|" + targetId + "|" + date, perTarget + amount);
            return new LimitDecision(true, p.dailyMax() - total - amount, p.perTargetMax() - perTarget - amount);
        }

        @Override
        public void refund(Long userId, Long targetId, LocalDate date, int amount) {
        }

        @Override
        public int remainingToday(Long userId, LocalDate date, HeartPolicy p) {
            return p.dailyMax() - used.getOrDefault(userId + "|" + date, 0);
        }
    };

    final Map<String, Set<Long>> deviceUsers = new HashMap<>();
    final AbuseSignalPort abuse = (date, targetId, userId, device, ip, age) -> {
        Set<Long> users = deviceUsers.computeIfAbsent(device + "|" + targetId, k -> new HashSet<>());
        users.add(userId);
        return new AbuseSignals(age, users.size(), 1);
    };

    final Match match = Match.schedule(1L, 9L, 1L, now, "잠실", 1L, now);
    final MatchRepositoryPort matchRepo = new MatchRepositoryPort() {
        @Override
        public Match save(Match m) {
            return m;
        }

        @Override
        public Optional<Match> findById(Long id) {
            return Optional.of(new Match(id, match.getLeagueId(), match.getHomeTargetId(), match.getAwayTargetId(),
                    match.getStartsAt(), match.getStatus(), 0, 0, "잠실", 1L, now, now));
        }

        @Override
        public List<Match> findByStartsAtBetween(Instant from, Instant to, Long leagueId) {
            return List.of();
        }

        @Override
        public List<Match> findLiveOrUpcoming(Instant from, int limit) {
            return List.of();
        }
    };
    final MatchGaugePort gauges = new MatchGaugePort() {
        @Override
        public void add(Long matchId, MatchSide side, long hearts) {
        }

        @Override
        public MatchGauge read(Long matchId) {
            return MatchGauge.empty();
        }
    };

    final java.util.List<HeartGrant> logged = new java.util.ArrayList<>();
    final HeartLogPort logs = new HeartLogPort() {
        @Override
        public void insertBatch(List<HeartGrant> grants) {
            logged.addAll(grants);
        }

        @Override
        public List<Supporter> findTopSupporters(Long targetId, Instant from, Instant to, int limit) {
            return List.of();
        }
    };
    final HeartAccumulator accumulator = new HeartAccumulator(logs, new Fakes.Stats(), gauges,
            (l, t, d, a) -> { }, new NoopTransactionManager());
    final HeartService service = new HeartService(limiter, abuse, accumulator, lookup,
            new MatchService(matchRepo, gauges, catalog, lookup, features, Fakes.properties(), clock), features,
            Fakes.properties(), clock);

    GiveCommand give(long user, long target, int amount, String device) {
        return new GiveCommand(user, "u" + user, now.minusSeconds(86_400L * 30), target, amount,
                HeartSource.TARGET_PAGE, null, device, "1.2.3.4");
    }

    @Test
    void 하루_5개_한_대상에_3개까지_줄_수_있다() {
        var first = service.give(give(1, 9L, 3, "d1"));
        assertThat(first.remainingToday()).isEqualTo(2);
        assertThat(first.remainingForTarget()).isZero();

        assertThatThrownBy(() -> service.give(give(1, 9L, 1, "d1"))).hasMessageContaining("하트를 모두 썼습니다");
        service.give(give(1, 1L, 2, "d1"));
        assertThatThrownBy(() -> service.give(give(1, 8L, 1, "d1"))).hasMessageContaining("하트를 모두 썼습니다");
        assertThat(service.remaining(1L).remainingToday()).isZero();
    }

    @Test
    void 한_번에_한_대상_한도보다_많이_줄_수_없다() {
        assertThatThrownBy(() -> service.give(give(1, 9L, 4, "d1"))).hasMessageContaining("1~3");
    }

    @Test
    void 같은_기기의_세_번째_계정부터는_의심_하트로_기록된다() {
        service.give(give(1, 9L, 1, "same-device"));
        service.give(give(2, 9L, 1, "same-device"));
        service.give(give(3, 9L, 1, "same-device"));
        accumulator.flush();

        assertThat(logged).extracting(HeartGrant::suspicious).containsExactly(false, false, true);
    }

    @Test
    void 경기_응원_하트는_그_경기에_나오는_팀에만_줄_수_있다() {
        GiveCommand toKia = new GiveCommand(1L, "u1", null, 8L, 1, HeartSource.MATCH, 5L, "d", "ip");
        assertThatThrownBy(() -> service.give(toKia)).hasMessageContaining("경기에 나오는 팀");

        service.give(new GiveCommand(1L, "u1", null, 9L, 1, HeartSource.MATCH, 5L, "d", "ip"));
        accumulator.flush();
        assertThat(logged.get(0).matchSide()).isEqualTo(MatchSide.HOME);
    }
}
