package com.livebroadcast.fandom.app.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livebroadcast.fandom.app.FandomProperties;
import com.livebroadcast.fandom.app.event.FanChangesApplied;
import com.livebroadcast.fandom.app.port.in.FanUseCase;
import com.livebroadcast.fandom.app.port.out.DomainEventPublisherPort;
import com.livebroadcast.fandom.app.port.out.FanRepositoryPort;
import com.livebroadcast.fandom.app.port.out.FanTargetRepositoryPort;
import com.livebroadcast.fandom.app.port.out.TargetStatsPort;
import com.livebroadcast.fandom.domain.catalog.League;
import com.livebroadcast.fandom.domain.fan.FanChange;
import com.livebroadcast.fandom.domain.fan.FanErrors;
import com.livebroadcast.fandom.domain.fan.FanMembership;
import com.livebroadcast.fandom.domain.fan.PrimaryFanPolicy;
import com.livebroadcast.fandom.domain.target.FanTarget;

@Service
public class FanService implements FanUseCase {

    private static final int MAX_BADGE_USERS = 100;

    private final FanRepositoryPort fans;
    private final FanTargetRepositoryPort targets;
    private final TargetStatsPort stats;
    private final TargetLookup lookup;
    private final DomainEventPublisherPort events;
    private final PrimaryFanPolicy policy;
    private final Clock clock;

    public FanService(FanRepositoryPort fans, FanTargetRepositoryPort targets, TargetStatsPort stats,
                      TargetLookup lookup, DomainEventPublisherPort events, FandomProperties properties, Clock clock) {
        this.fans = fans;
        this.targets = targets;
        this.stats = stats;
        this.lookup = lookup;
        this.events = events;
        this.policy = properties.primaryFanPolicy();
        this.clock = clock;
    }

    @Override
    @Transactional
    public FanMembership join(Long userId, String nickname, Long targetId, boolean primary) {
        fans.lockUser(userId);
        FanTarget target = lookup.visibleTarget(targetId);
        Long leagueId = target.getLeagueId();
        Instant now = clock.instant();
        Optional<FanMembership> existing = fans.find(userId, targetId);
        List<FanChange> changes = new ArrayList<>();
        FanMembership result;

        if (primary) {
            Optional<FanMembership> currentPrimary = fans.findPrimary(userId, leagueId);
            if (currentPrimary.isPresent() && currentPrimary.get().targetId().equals(targetId)) {
                return currentPrimary.get();
            }
            policy.ensureCanChange(fans.findLastPrimaryChange(userId, leagueId).orElse(null), now);
            currentPrimary.ifPresent(old -> {
                // 예전 주 응원팀은 관심 팬으로 남긴다(원하면 사용자가 따로 해제).
                fans.save(old.asPrimary(false));
                changes.add(new FanChange(old.targetId(), -1, 1));
            });
            if (existing.isPresent()) {
                result = fans.save(existing.get().asPrimary(true));
                changes.add(new FanChange(targetId, 1, -1));
            } else {
                result = fans.save(new FanMembership(userId, targetId, leagueId, nickname, true, now));
                changes.add(new FanChange(targetId, 1, 0));
            }
            fans.recordPrimaryChange(userId, leagueId, now);
        } else if (existing.isPresent()) {
            FanMembership current = existing.get();
            if (!current.primary()) {
                return current;
            }
            policy.ensureCanLeave(fans.findLastPrimaryChange(userId, leagueId).orElse(null), now);
            result = fans.save(current.asPrimary(false));
            changes.add(new FanChange(targetId, -1, 1));
        } else {
            result = fans.save(new FanMembership(userId, targetId, leagueId, nickname, false, now));
            changes.add(new FanChange(targetId, 0, 1));
        }
        apply(leagueId, changes, now);
        return result;
    }

    @Override
    @Transactional
    public void leave(Long userId, Long targetId) {
        fans.lockUser(userId);
        FanMembership membership = fans.find(userId, targetId).orElseThrow(FanErrors::notFan);
        Instant now = clock.instant();
        if (membership.primary()) {
            policy.ensureCanLeave(fans.findLastPrimaryChange(userId, membership.leagueId()).orElse(null), now);
        }
        fans.delete(userId, targetId);
        apply(membership.leagueId(),
                List.of(membership.primary() ? new FanChange(targetId, -1, 0) : new FanChange(targetId, 0, -1)), now);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyFan> myFans(Long userId) {
        List<FanMembership> list = fans.findByUser(userId);
        Map<Long, FanTarget> targetMap = new LinkedHashMap<>();
        targets.findByIds(list.stream().map(FanMembership::targetId).toList())
                .forEach(t -> targetMap.put(t.getId(), t));
        return list.stream()
                .filter(m -> targetMap.containsKey(m.targetId()))
                .map(m -> {
                    League league = lookup.league(m.leagueId());
                    Instant last = fans.findLastPrimaryChange(userId, m.leagueId()).orElse(null);
                    return new MyFan(m, targetMap.get(m.targetId()), league, policy.availableAt(last));
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<Badge>> badges(Collection<Long> userIds) {
        List<Long> ids = userIds.stream().distinct().limit(MAX_BADGE_USERS).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<FanMembership> primaries = fans.findPrimaryByUsers(ids);
        Map<Long, FanTarget> targetMap = new LinkedHashMap<>();
        targets.findByIds(primaries.stream().map(FanMembership::targetId).distinct().toList())
                .forEach(t -> targetMap.put(t.getId(), t));
        Map<Long, List<Badge>> result = new LinkedHashMap<>();
        for (FanMembership m : primaries) {
            FanTarget t = targetMap.get(m.targetId());
            if (t == null || !t.getStatus().isVisible()) {
                continue;
            }
            League league = lookup.league(t.getLeagueId());
            result.computeIfAbsent(m.userId(), k -> new ArrayList<>()).add(new Badge(t.getId(),
                    t.getProfile().shortName(), t.getProfile().colorPrimary(), t.getProfile().colorSecondary(),
                    league.code()));
        }
        return result;
    }

    /** 신청이 승인됐을 때 신청자와 투표자를 관심 팬으로 등록한다(이미 팬이면 그대로). */
    @Transactional
    public int followAll(Long targetId, Long leagueId, List<Follower> followers) {
        Instant now = clock.instant();
        int added = 0;
        for (Follower f : followers) {
            if (fans.find(f.userId(), targetId).isEmpty()) {
                fans.save(new FanMembership(f.userId(), targetId, leagueId, f.nickname(), false, now));
                added++;
            }
        }
        if (added > 0) {
            apply(leagueId, List.of(new FanChange(targetId, 0, added)), now);
        }
        return added;
    }

    public record Follower(Long userId, String nickname) {
    }

    private void apply(Long leagueId, List<FanChange> changes, Instant now) {
        List<FanChange> effective = changes.stream().filter(c -> !c.isEmpty()).toList();
        if (effective.isEmpty()) {
            return;
        }
        stats.applyFanChanges(effective, now);
        events.publish(new FanChangesApplied(leagueId, effective, now));
    }
}
