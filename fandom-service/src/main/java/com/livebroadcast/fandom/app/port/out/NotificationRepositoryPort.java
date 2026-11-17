package com.livebroadcast.fandom.app.port.out;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import com.livebroadcast.fandom.app.PageResult;
import com.livebroadcast.fandom.domain.notification.BroadcastLiveEvent;
import com.livebroadcast.fandom.domain.notification.Notification;
import com.livebroadcast.fandom.domain.notification.NotificationType;

public interface NotificationRepositoryPort {

    /**
     * 응원팀 팬 전원에게 알림을 한 번에 만든다(INSERT ... SELECT, 팬 수만큼 왕복하지 않는다).
     * (user_id, event_id) 유일 제약 덕분에 같은 이벤트를 다시 처리해도 늘어나지 않는다.
     *
     * @return 이번에 새로 알림을 받은 사용자 ID
     */
    List<Long> fanOutToFans(BroadcastLiveEvent event, Instant now);

    /** 사용자 몇 명에게 같은 알림을 만든다(승부 예측 결과 등). */
    void insertForUsers(Collection<Long> userIds, String eventId, NotificationType type, String title, String body,
                        String link, Long targetId, Instant now);

    PageResult<Notification> findByUser(Long userId, int page, int size);

    long countUnread(Long userId);

    /** ids 가 비어 있으면 전부 읽음 처리 */
    int markRead(Long userId, Collection<Long> ids, Instant now);

    /** 오래된 읽은 알림 정리 */
    int deleteReadBefore(Instant before);
}
