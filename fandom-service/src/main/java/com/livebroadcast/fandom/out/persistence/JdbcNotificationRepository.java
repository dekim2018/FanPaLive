package com.livebroadcast.fandom.out.persistence;

import static com.livebroadcast.fandom.out.persistence.JdbcSupport.instant;
import static com.livebroadcast.fandom.out.persistence.JdbcSupport.nullableLong;
import static com.livebroadcast.fandom.out.persistence.JdbcSupport.ts;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import com.livebroadcast.fandom.app.PageResult;
import com.livebroadcast.fandom.app.port.out.NotificationRepositoryPort;
import com.livebroadcast.fandom.domain.notification.BroadcastLiveEvent;
import com.livebroadcast.fandom.domain.notification.Notification;
import com.livebroadcast.fandom.domain.notification.NotificationType;

@Component
public class JdbcNotificationRepository implements NotificationRepositoryPort {

    private static final RowMapper<Notification> MAPPER = (rs, i) -> new Notification(rs.getLong("id"),
            rs.getLong("user_id"), NotificationType.valueOf(rs.getString("type")), rs.getString("title"),
            rs.getString("body"), rs.getString("link"), nullableLong(rs, "target_id"), instant(rs, "created_at"),
            instant(rs, "read_at"));

    private final JdbcTemplate jdbc;

    public JdbcNotificationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Long> fanOutToFans(BroadcastLiveEvent e, Instant now) {
        // 응원팀 팬 전원에게 한 문장으로 만든다. 방송자 본인은 제외한다.
        return jdbc.queryForList("""
                INSERT INTO notifications (user_id, event_id, type, title, body, link, target_id, created_at)
                SELECT fm.user_id, ?, 'BROADCAST_LIVE', ?, ?, ?, fm.target_id, ?
                FROM fan_memberships fm
                WHERE fm.target_id = ? AND fm.user_id <> ?
                ON CONFLICT (user_id, event_id) DO NOTHING
                RETURNING user_id
                """, Long.class, e.eventId(), e.notificationTitle(), e.notificationBody(), e.link(), ts(now),
                e.targetId(), e.ownerId() == null ? -1L : e.ownerId());
    }

    @Override
    public void insertForUsers(Collection<Long> userIds, String eventId, NotificationType type, String title,
                               String body, String link, Long targetId, Instant now) {
        jdbc.batchUpdate("""
                INSERT INTO notifications (user_id, event_id, type, title, body, link, target_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id, event_id) DO NOTHING
                """, userIds.stream().map(id -> new Object[]{id, eventId, type.name(), title, body, link, targetId,
                ts(now)}).toList());
    }

    @Override
    public PageResult<Notification> findByUser(Long userId, int page, int size) {
        List<Notification> content = jdbc.query("""
                SELECT * FROM notifications WHERE user_id = ? ORDER BY id DESC LIMIT ? OFFSET ?
                """, MAPPER, userId, size, (long) page * size);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE user_id = ?", Long.class, userId);
        long t = total == null ? 0 : total;
        return new PageResult<>(content, page, size, t, (int) Math.ceil(t / (double) size));
    }

    @Override
    public long countUnread(Long userId) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND read_at IS NULL",
                Long.class, userId);
        return n == null ? 0 : n;
    }

    @Override
    public int markRead(Long userId, Collection<Long> ids, Instant now) {
        if (ids.isEmpty()) {
            return jdbc.update("UPDATE notifications SET read_at = ? WHERE user_id = ? AND read_at IS NULL",
                    ts(now), userId);
        }
        List<Object> args = new ArrayList<>();
        args.add(ts(now));
        args.add(userId);
        args.addAll(ids.stream().limit(200).toList());
        String marks = String.join(",", ids.stream().limit(200).map(x -> "?").toList());
        return jdbc.update("UPDATE notifications SET read_at = ? WHERE user_id = ? AND read_at IS NULL AND id IN ("
                + marks + ")", args.toArray());
    }

    @Override
    public int deleteReadBefore(Instant before) {
        return jdbc.update("DELETE FROM notifications WHERE read_at IS NOT NULL AND created_at < ?", ts(before));
    }
}
