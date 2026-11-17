package com.livebroadcast.fandom.app.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.livebroadcast.common.error.DomainException;
import com.livebroadcast.fandom.app.port.in.NotificationUseCase;
import com.livebroadcast.fandom.app.port.out.NotificationPushPort;
import com.livebroadcast.fandom.app.port.out.NotificationRepositoryPort;
import com.livebroadcast.fandom.app.port.out.StreamTicketPort;
import com.livebroadcast.fandom.domain.notification.BroadcastLiveEvent;

/**
 * 응원팀 편파방 시작 알림.
 *
 * 팬이 많은 팀은 알림 한 번에 수천 명이 대상이다. 팬마다 INSERT 하면 왕복이 팬 수만큼 생기므로
 * fan_memberships 에서 바로 INSERT ... SELECT 로 한 번에 만들고, 실시간 전송은 접속 중인 사람에게만 한다.
 * 저장(커밋)이 끝난 뒤에 보내서, 롤백된 알림이 화면에 뜨지 않게 한다.
 */
@Service
public class NotificationService implements NotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    static final Duration KEEP_READ = Duration.ofDays(30);

    private final NotificationRepositoryPort notifications;
    private final NotificationPushPort push;
    private final StreamTicketPort tickets;
    private final TransactionTemplate tx;
    private final Clock clock;

    public NotificationService(NotificationRepositoryPort notifications, NotificationPushPort push,
                               StreamTicketPort tickets, PlatformTransactionManager transactionManager, Clock clock) {
        this.notifications = notifications;
        this.push = push;
        this.tickets = tickets;
        this.tx = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationPage list(Long userId, int page, int size) {
        int safeSize = size <= 0 ? 20 : Math.min(size, 50);
        return new NotificationPage(notifications.findByUser(userId, Math.max(page, 0), safeSize),
                notifications.countUnread(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notifications.countUnread(userId);
    }

    @Override
    @Transactional
    public int markRead(Long userId, Collection<Long> ids) {
        return notifications.markRead(userId, ids == null ? List.of() : ids, clock.instant());
    }

    @Override
    public int onBroadcastLive(BroadcastLiveEvent event) {
        if (event.targetId() == null) {
            return 0;
        }
        Instant now = clock.instant();
        List<Long> recipients = tx.execute(status -> notifications.fanOutToFans(event, now));
        if (recipients == null || recipients.isEmpty()) {
            return 0;
        }
        try {
            push.pushNew(recipients, event.notificationTitle(), event.notificationBody(), event.link());
        } catch (RuntimeException e) {
            // 실시간 전송이 실패해도 알림은 저장되어 있어 목록에서 볼 수 있다.
            log.debug("알림 실시간 전송 실패 reason={}", e.getMessage());
        }
        log.info("편파방 시작 알림 broadcastId={} targetId={} 대상={}명", event.broadcastId(), event.targetId(),
                recipients.size());
        return recipients.size();
    }

    @Override
    public String issueStreamTicket(Long userId) {
        return tickets.issue(userId);
    }

    @Override
    public Long redeemStreamTicket(String ticket) {
        if (ticket == null || !ticket.matches("[A-Za-z0-9_-]{16,64}")) {
            throw invalidTicket();
        }
        return tickets.redeem(ticket).orElseThrow(NotificationService::invalidTicket);
    }

    @Transactional
    public int cleanup() {
        return notifications.deleteReadBefore(clock.instant().minus(KEEP_READ));
    }

    private static DomainException invalidTicket() {
        return DomainException.unauthorized("INVALID_STREAM_TICKET", "알림 연결이 만료되었습니다. 새로고침해 주세요.");
    }
}
