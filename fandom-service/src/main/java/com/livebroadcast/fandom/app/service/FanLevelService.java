package com.livebroadcast.fandom.app.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livebroadcast.fandom.app.event.DonationCredited;
import com.livebroadcast.fandom.app.port.in.FanLevelUseCase;
import com.livebroadcast.fandom.app.port.out.PointLedgerPort;
import com.livebroadcast.fandom.domain.level.FanProfile;
import com.livebroadcast.fandom.domain.level.PointReason;

/**
 * 팬 포인트와 등급.
 *
 * 포인트는 원장(fan_point_ledger)에 근거와 함께 쌓고, (사용자, 이유, 근거) 유일 제약으로 중복 적립을 막는다.
 * 하트는 요청마다 적립하면 하트 경로에 쓰기가 하나 더 붙으므로, 새벽에 하루치를 사용자별로 합쳐 한 번에 넣는다.
 */
@Service
public class FanLevelService implements FanLevelUseCase {

    static final int MAX_PROFILES = 100;

    private final PointLedgerPort ledger;
    private final Clock clock;

    public FanLevelService(PointLedgerPort ledger, Clock clock) {
        this.ledger = ledger;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public FanProfile profile(Long userId) {
        return ledger.findProfile(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, FanProfile> profiles(Collection<Long> userIds) {
        return ledger.findProfiles(userIds.stream().distinct().limit(MAX_PROFILES).toList());
    }

    @Override
    @Transactional
    public int awardDailyHearts(LocalDate date) {
        return ledger.awardDailyHearts(date, PointReason.POINTS_PER_HEART, clock.instant());
    }

    /** 후원 집계 반영과 같은 트랜잭션에서 적립한다(후원이 반영되면 포인트도 반드시 쌓인다). */
    @EventListener
    public void on(DonationCredited event) {
        ledger.award(event.donorId(), event.donorNickname(), event.targetId(), PointReason.DONATION, event.eventId(),
                PointReason.donationPoints(event.amount()), clock.instant());
    }
}
