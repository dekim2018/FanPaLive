package com.livebroadcast.fandom.app.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livebroadcast.fandom.app.FandomProperties;
import com.livebroadcast.fandom.app.event.DonationApplied;
import com.livebroadcast.fandom.app.port.in.DonationEventUseCase;
import com.livebroadcast.fandom.app.port.out.DomainEventPublisherPort;
import com.livebroadcast.fandom.app.port.out.DonationContributionPort;
import com.livebroadcast.fandom.app.port.out.FanTargetRepositoryPort;
import com.livebroadcast.fandom.app.port.out.TargetStatsPort;
import com.livebroadcast.fandom.domain.donation.DonationEvent;
import com.livebroadcast.fandom.domain.ranking.PeriodKeys;
import com.livebroadcast.fandom.domain.ranking.ScoreFormula;
import com.livebroadcast.fandom.domain.target.FanTarget;

/**
 * 후원 확정 이벤트를 팬덤 집계에 반영한다.
 * 이벤트 ID 를 PK 로 저장해서, 같은 이벤트가 몇 번 와도 집계는 한 번만 늘어난다(멱등 소비자).
 */
@Service
public class DonationEventService implements DonationEventUseCase {

    private static final Logger log = LoggerFactory.getLogger(DonationEventService.class);

    private final DonationContributionPort contributions;
    private final FanTargetRepositoryPort targets;
    private final TargetStatsPort stats;
    private final DomainEventPublisherPort events;
    private final PeriodKeys keys;

    public DonationEventService(DonationContributionPort contributions, FanTargetRepositoryPort targets,
                                TargetStatsPort stats, DomainEventPublisherPort events, FandomProperties properties) {
        this.contributions = contributions;
        this.targets = targets;
        this.stats = stats;
        this.events = events;
        this.keys = properties.periodKeys();
    }

    @Override
    @Transactional
    public boolean apply(DonationEvent event) {
        if (event.targetId() == null || event.amount() <= 0) {
            return false;
        }
        Optional<FanTarget> target = targets.findById(event.targetId());
        if (target.isEmpty()) {
            log.warn("알 수 없는 대상의 후원 이벤트를 건너뜁니다 eventId={} targetId={}", event.eventId(), event.targetId());
            return false;
        }
        if (!contributions.insertIfAbsent(event)) {
            return false;
        }
        stats.addDonation(event.targetId(), keys.dateOf(event.approvedAt()), event.amount(), event.approvedAt());
        events.publish(new DonationApplied(target.get().getLeagueId(), event.targetId(),
                ScoreFormula.donationPoints(event.amount()), event.approvedAt()));
        return true;
    }
}
