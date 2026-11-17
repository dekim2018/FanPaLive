package com.livebroadcast.fandom.app.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.livebroadcast.fandom.app.FandomProperties;
import com.livebroadcast.fandom.app.FeatureGuard;
import com.livebroadcast.fandom.app.port.in.HeartUseCase;
import com.livebroadcast.fandom.app.port.out.AbuseSignalPort;
import com.livebroadcast.fandom.app.port.out.HeartLimiterPort;
import com.livebroadcast.fandom.domain.heart.AbusePolicy;
import com.livebroadcast.fandom.domain.heart.AbuseSignals;
import com.livebroadcast.fandom.domain.heart.HeartErrors;
import com.livebroadcast.fandom.domain.heart.HeartGrant;
import com.livebroadcast.fandom.domain.heart.HeartPolicy;
import com.livebroadcast.fandom.domain.heart.HeartResult;
import com.livebroadcast.fandom.domain.heart.HeartSource;
import com.livebroadcast.fandom.domain.heart.HeartWeight;
import com.livebroadcast.fandom.domain.heart.LimitDecision;
import com.livebroadcast.fandom.domain.match.Match;
import com.livebroadcast.fandom.domain.match.MatchSide;
import com.livebroadcast.fandom.domain.ranking.PeriodKeys;
import com.livebroadcast.fandom.domain.target.FanTarget;

/**
 * 하트 요청 처리. 요청 스레드에서 하는 일은 한도 검사(Redis Lua 1회), 어뷰징 신호 기록, 큐에 넣기뿐이다.
 * DB 기록과 랭킹 반영은 {@link HeartAccumulator} 가 200ms 마다 모아서 한다.
 *
 * 내구성 등급: 하트는 "최종 일관성, 장애 시 최대 200ms 분량 유실 허용" 등급이다(docs/adr/0001).
 * Redis 가 죽으면 한도를 검사할 수 없으므로 하트만 잠시 막고, 방송과 시청은 영향을 받지 않는다.
 */
@Service
public class HeartService implements HeartUseCase {

    private static final Logger log = LoggerFactory.getLogger(HeartService.class);

    private final HeartLimiterPort limiter;
    private final AbuseSignalPort abuseSignals;
    private final HeartAccumulator accumulator;
    private final TargetLookup lookup;
    private final MatchService matches;
    private final FeatureGuard features;
    private final HeartPolicy policy;
    private final AbusePolicy abusePolicy;
    private final PeriodKeys periodKeys;
    private final Clock clock;

    public HeartService(HeartLimiterPort limiter, AbuseSignalPort abuseSignals, HeartAccumulator accumulator,
                        TargetLookup lookup, MatchService matches, FeatureGuard features,
                        FandomProperties properties, Clock clock) {
        this.limiter = limiter;
        this.abuseSignals = abuseSignals;
        this.accumulator = accumulator;
        this.lookup = lookup;
        this.matches = matches;
        this.features = features;
        this.policy = properties.heartPolicy();
        this.abusePolicy = properties.abusePolicy();
        this.periodKeys = properties.periodKeys();
        this.clock = clock;
    }

    @Override
    public HeartResult give(GiveCommand command) {
        features.hearts();
        policy.validateAmount(command.amount());
        FanTarget target = lookup.visibleTarget(command.targetId());
        HeartSource source = command.source() == null ? HeartSource.TARGET_PAGE : command.source();

        MatchSide side = null;
        if (source == HeartSource.MATCH) {
            if (command.matchId() == null) {
                throw HeartErrors.matchRequired();
            }
            Match match = matches.cheerable(command.matchId());
            side = match.sideOf(target.getId());
        }

        Instant now = clock.instant();
        LocalDate date = periodKeys.dateOf(now);
        LimitDecision decision;
        try {
            decision = limiter.tryConsume(command.userId(), target.getId(), date, command.amount(), policy);
        } catch (RuntimeException e) {
            log.warn("하트 한도 저장소 오류로 하트를 막습니다. reason={}", e.getMessage());
            throw HeartErrors.unavailable();
        }
        if (!decision.allowed()) {
            throw HeartErrors.dailyLimit(policy.dailyMax(), policy.perTargetMax());
        }

        HeartWeight weight = abusePolicy.weigh(signals(command, target.getId(), date, now));
        HeartGrant grant = new HeartGrant(command.userId(), command.nickname(), target.getId(), target.getLeagueId(),
                command.matchId(), side, command.amount(), weight.pointsPerHeart(), weight.suspicious(), source,
                date, now);
        if (weight.suspicious()) {
            log.info("의심 하트 userId={} targetId={} reason={}", command.userId(), target.getId(), weight.reason());
        }
        if (!accumulator.enqueue(grant)) {
            limiter.refund(command.userId(), target.getId(), date, command.amount());
            throw HeartErrors.unavailable();
        }
        return new HeartResult(command.amount(), decision.remainingToday(), decision.remainingForTarget());
    }

    @Override
    public Remaining remaining(Long userId) {
        LocalDate date = periodKeys.dateOf(clock.instant());
        int remaining;
        try {
            remaining = limiter.remainingToday(userId, date, policy);
        } catch (RuntimeException e) {
            remaining = 0;
        }
        return new Remaining(policy.dailyMax(), policy.perTargetMax(), remaining);
    }

    private AbuseSignals signals(GiveCommand command, Long targetId, LocalDate date, Instant now) {
        long ageDays = command.joinedAt() == null ? -1 : Duration.between(command.joinedAt(), now).toDays();
        try {
            return abuseSignals.record(date, targetId, command.userId(), hash(command.deviceId()),
                    hash(command.clientIp()), ageDays);
        } catch (RuntimeException e) {
            // 신호를 못 모으면 계정 나이만으로 판단한다.
            return new AbuseSignals(ageDays, 1, 1);
        }
    }

    /** 기기 ID 와 IP 원문은 저장하지 않는다. 같은지 비교만 하면 되므로 해시 앞부분만 쓴다. */
    static String hash(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.strip().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
