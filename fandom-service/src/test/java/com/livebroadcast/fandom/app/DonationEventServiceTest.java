package com.livebroadcast.fandom.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.livebroadcast.fandom.app.event.DonationApplied;
import com.livebroadcast.fandom.app.port.out.DonationContributionPort;
import com.livebroadcast.fandom.app.service.DonationEventService;
import com.livebroadcast.fandom.domain.donation.DonationEvent;
import com.livebroadcast.fandom.support.Fakes;

/** 같은 후원 이벤트가 여러 번 와도(재전송, 재처리) 집계는 한 번만 늘어난다 */
class DonationEventServiceTest {

    final Fakes.Stats stats = new Fakes.Stats();
    final Fakes.Events events = new Fakes.Events();
    final Set<String> seen = new HashSet<>();
    final DonationContributionPort contributions = e -> seen.add(e.eventId());
    final DonationEventService service = new DonationEventService(contributions,
            new Fakes.Targets().add(Fakes.team(9L, Fakes.KBO, "두산 베어스", "두산")), stats, events,
            Fakes.properties());

    // 2026-11-17 23:30 KST
    final DonationEvent event = new DonationEvent("donation-42", 9L, 3L, "곰돌이", 5000,
            Instant.parse("2026-11-17T14:30:00Z"));

    @Test
    void 처음_온_이벤트만_반영한다() {
        assertThat(service.apply(event)).isTrue();
        assertThat(service.apply(event)).isFalse();
        assertThat(service.apply(event)).isFalse();

        assertThat(stats.findStats(List.of(9L)).get(9L).donationTotal()).isEqualTo(5000L);
        // 집계 반영(랭킹 점수)과 후원한 팬의 포인트 적립 이벤트가 하나씩 나간다
        assertThat(events.published).filteredOn(DonationApplied.class::isInstance).hasSize(1);
        assertThat(events.published).filteredOn(com.livebroadcast.fandom.app.event.DonationCredited.class::isInstance)
                .hasSize(1);
        assertThat(((DonationApplied) events.published.stream().filter(DonationApplied.class::isInstance)
                .findFirst().orElseThrow()).points()).isEqualTo(50L);
    }

    @Test
    void 날짜는_KST_기준으로_나눈다() {
        service.apply(new DonationEvent("donation-43", 9L, 3L, "곰돌이", 1000, Instant.parse("2026-11-17T15:30:00Z")));

        assertThat(stats.findDailyDonations(LocalDate.of(2026, 11, 18)).get(9L)).isEqualTo(1000L);
    }

    @Test
    void 모르는_대상의_이벤트는_건너뛴다() {
        assertThat(service.apply(new DonationEvent("donation-44", 404L, 3L, "a", 1000, Instant.now()))).isFalse();
        assertThat(seen).isEmpty();
    }
}
