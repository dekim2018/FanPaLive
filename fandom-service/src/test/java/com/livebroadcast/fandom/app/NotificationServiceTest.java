package com.livebroadcast.fandom.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.livebroadcast.fandom.app.service.NotificationService;
import com.livebroadcast.fandom.domain.notification.BroadcastLiveEvent;
import com.livebroadcast.fandom.support.ExtrasFakes;
import com.livebroadcast.fandom.support.NoopTransactionManager;

class NotificationServiceTest {

    final Instant now = Instant.parse("2026-11-17T09:00:00Z");
    final ExtrasFakes.Notifications repo = new ExtrasFakes.Notifications();
    final ExtrasFakes.Push push = new ExtrasFakes.Push();
    final ExtrasFakes.Tickets tickets = new ExtrasFakes.Tickets();
    final NotificationService service = new NotificationService(repo, push, tickets, new NoopTransactionManager(),
            Clock.fixed(now, ZoneOffset.UTC));

    BroadcastLiveEvent event() {
        return new BroadcastLiveEvent("broadcast-live-7-1", 7L, 9L, "두산 베어스", "오늘은 잠실 직관 분위기로", 3L, "방장",
                now);
    }

    @Test
    void 응원팀_팬에게만_알리고_방송자_본인은_빠진다() {
        repo.fans.put(9L, List.of(1L, 2L, 3L));

        int sent = service.onBroadcastLive(event());

        assertThat(sent).isEqualTo(2);
        assertThat(push.pushed).containsExactly(1L, 2L);
        assertThat(repo.rows.get(0).title()).isEqualTo("두산 베어스 편파방이 시작됐어요");
        assertThat(repo.rows.get(0).body()).isEqualTo("방장: 오늘은 잠실 직관 분위기로");
    }

    @Test
    void 같은_이벤트가_두_번_와도_알림은_한_번이다() {
        repo.fans.put(9L, List.of(1L));

        service.onBroadcastLive(event());
        int again = service.onBroadcastLive(event());

        assertThat(again).isZero();
        assertThat(repo.rows).hasSize(1);
    }

    @Test
    void 실시간_전송이_실패해도_알림은_남는다() {
        repo.fans.put(9L, List.of(1L));
        push.fail = true;

        service.onBroadcastLive(event());

        assertThat(service.unreadCount(1L)).isEqualTo(1);
        service.markRead(1L, List.of());
        assertThat(service.unreadCount(1L)).isZero();
    }

    @Test
    void 구독_티켓은_한_번만_쓸_수_있다() {
        String ticket = service.issueStreamTicket(5L);

        assertThat(service.redeemStreamTicket(ticket)).isEqualTo(5L);
        assertThatThrownBy(() -> service.redeemStreamTicket(ticket)).hasMessageContaining("만료");
        assertThatThrownBy(() -> service.redeemStreamTicket("../../etc")).hasMessageContaining("만료");
    }
}
