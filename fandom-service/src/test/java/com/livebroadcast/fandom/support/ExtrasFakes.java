package com.livebroadcast.fandom.support;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import com.livebroadcast.fandom.app.PageResult;
import com.livebroadcast.fandom.app.port.out.MatchRepositoryPort;
import com.livebroadcast.fandom.app.port.out.NotificationPushPort;
import com.livebroadcast.fandom.app.port.out.NotificationRepositoryPort;
import com.livebroadcast.fandom.app.port.out.PointLedgerPort;
import com.livebroadcast.fandom.app.port.out.PredictionRepositoryPort;
import com.livebroadcast.fandom.app.port.out.StreamTicketPort;
import com.livebroadcast.fandom.domain.level.FanProfile;
import com.livebroadcast.fandom.domain.level.PointReason;
import com.livebroadcast.fandom.domain.match.Match;
import com.livebroadcast.fandom.domain.match.MatchSide;
import com.livebroadcast.fandom.domain.notification.BroadcastLiveEvent;
import com.livebroadcast.fandom.domain.notification.Notification;
import com.livebroadcast.fandom.domain.notification.NotificationType;
import com.livebroadcast.fandom.domain.prediction.Prediction;
import com.livebroadcast.fandom.domain.prediction.PredictionRules.Outcome;
import com.livebroadcast.fandom.domain.prediction.PredictionTally;

/** 알림, 포인트, 예측, 경기 저장소 가짜 구현 */
public final class ExtrasFakes {

    private ExtrasFakes() {
    }

    public static class Matches implements MatchRepositoryPort {
        public final Map<Long, Match> byId = new LinkedHashMap<>();
        final AtomicLong seq = new AtomicLong();

        @Override
        public Match save(Match m) {
            Match saved = m.getId() != null ? m : new Match(seq.incrementAndGet(), m.getLeagueId(),
                    m.getHomeTargetId(), m.getAwayTargetId(), m.getStartsAt(), m.getStatus(), m.getHomeScore(),
                    m.getAwayScore(), m.getVenue(), m.getCreatedBy(), m.getCreatedAt(), m.getUpdatedAt());
            byId.put(saved.getId(), saved);
            return saved;
        }

        @Override
        public Optional<Match> findById(Long id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public List<Match> findByStartsAtBetween(Instant from, Instant to, Long leagueId) {
            return byId.values().stream().filter(m -> m.getLeagueId().equals(leagueId)
                    && !m.getStartsAt().isBefore(from) && m.getStartsAt().isBefore(to)).toList();
        }

        @Override
        public List<Match> findLiveOrUpcoming(Instant from, int limit) {
            return List.of();
        }
    }

    public static class Predictions implements PredictionRepositoryPort {
        public final Map<String, Prediction> rows = new LinkedHashMap<>();

        @Override
        public void upsert(Long matchId, Long userId, String nickname, MatchSide pick, Instant now) {
            Prediction old = rows.get(matchId + ":" + userId);
            if (old != null && old.result() != null) {
                return;
            }
            rows.put(matchId + ":" + userId, new Prediction(matchId, userId, nickname, pick, now, null, null));
        }

        @Override
        public Optional<Prediction> find(Long matchId, Long userId) {
            return Optional.ofNullable(rows.get(matchId + ":" + userId));
        }

        @Override
        public PredictionTally tally(Long matchId) {
            long home = rows.values().stream().filter(p -> p.matchId().equals(matchId) && p.pick() == MatchSide.HOME)
                    .count();
            long away = rows.values().stream().filter(p -> p.matchId().equals(matchId) && p.pick() == MatchSide.AWAY)
                    .count();
            return new PredictionTally(home, away);
        }

        @Override
        public List<Prediction> settle(Long matchId, Outcome outcome, Instant now) {
            List<Prediction> changed = new ArrayList<>();
            rows.replaceAll((k, p) -> {
                if (!p.matchId().equals(matchId) || p.result() != null) {
                    return p;
                }
                Prediction s = new Prediction(p.matchId(), p.userId(), p.nickname(), p.pick(), p.createdAt(),
                        outcome.resultOf(p.pick()), now);
                changed.add(s);
                return s;
            });
            return changed;
        }

        @Override
        public List<Prediction> findByUser(Long userId, int limit) {
            return rows.values().stream().filter(p -> p.userId().equals(userId)).limit(limit).toList();
        }

        @Override
        public int[] hitsAndTotal(Long userId) {
            return new int[]{0, 0};
        }
    }

    public static class Ledger implements PointLedgerPort {
        public final Map<String, Integer> entries = new LinkedHashMap<>();
        public final Map<Long, Long> points = new HashMap<>();

        @Override
        public boolean award(Long userId, String nickname, Long targetId, PointReason reason, String refKey,
                             int amount, Instant now) {
            if (entries.putIfAbsent(userId + ":" + reason + ":" + refKey, amount) != null) {
                return false;
            }
            points.merge(userId, (long) amount, Long::sum);
            return true;
        }

        @Override
        public int awardDailyHearts(LocalDate date, int pointsPerHeart, Instant now) {
            return 0;
        }

        @Override
        public FanProfile findProfile(Long userId) {
            return new FanProfile(userId, null, points.getOrDefault(userId, 0L), 0, 0, 0, 0);
        }

        @Override
        public Map<Long, FanProfile> findProfiles(Collection<Long> userIds) {
            Map<Long, FanProfile> map = new HashMap<>();
            userIds.forEach(id -> map.put(id, findProfile(id)));
            return map;
        }
    }

    public static class Notifications implements NotificationRepositoryPort {
        public final List<Notification> rows = new ArrayList<>();
        public final Map<Long, List<Long>> fans = new HashMap<>();
        final AtomicLong seq = new AtomicLong();

        @Override
        public List<Long> fanOutToFans(BroadcastLiveEvent e, Instant now) {
            List<Long> created = new ArrayList<>();
            for (Long userId : fans.getOrDefault(e.targetId(), List.of())) {
                if (userId.equals(e.ownerId()) || exists(userId, e.eventId())) {
                    continue;
                }
                rows.add(new Notification(seq.incrementAndGet(), userId, NotificationType.BROADCAST_LIVE,
                        e.notificationTitle(), e.notificationBody(), e.link() + "#" + e.eventId(), e.targetId(), now,
                        null));
                created.add(userId);
            }
            return created;
        }

        private boolean exists(Long userId, String eventId) {
            return rows.stream().anyMatch(n -> n.userId().equals(userId) && n.link() != null
                    && n.link().endsWith("#" + eventId));
        }

        @Override
        public void insertForUsers(Collection<Long> userIds, String eventId, NotificationType type, String title,
                                   String body, String link, Long targetId, Instant now) {
            for (Long id : userIds) {
                if (!exists(id, eventId)) {
                    rows.add(new Notification(seq.incrementAndGet(), id, type, title, body, link + "#" + eventId,
                            targetId, now, null));
                }
            }
        }

        @Override
        public PageResult<Notification> findByUser(Long userId, int page, int size) {
            List<Notification> mine = rows.stream().filter(n -> n.userId().equals(userId)).toList();
            return new PageResult<>(mine, page, size, mine.size(), 1);
        }

        @Override
        public long countUnread(Long userId) {
            return rows.stream().filter(n -> n.userId().equals(userId) && n.readAt() == null).count();
        }

        @Override
        public int markRead(Long userId, Collection<Long> ids, Instant now) {
            int[] n = {0};
            rows.replaceAll(x -> {
                if (x.userId().equals(userId) && x.readAt() == null && (ids.isEmpty() || ids.contains(x.id()))) {
                    n[0]++;
                    return new Notification(x.id(), x.userId(), x.type(), x.title(), x.body(), x.link(),
                            x.targetId(), x.createdAt(), now);
                }
                return x;
            });
            return n[0];
        }

        @Override
        public int deleteReadBefore(Instant before) {
            int size = rows.size();
            rows.removeIf(n -> n.readAt() != null && n.createdAt().isBefore(before));
            return size - rows.size();
        }
    }

    public static class Push implements NotificationPushPort {
        public final List<Long> pushed = new ArrayList<>();
        public boolean fail;

        @Override
        public void pushNew(Collection<Long> userIds, String title, String body, String link) {
            if (fail) {
                throw new IllegalStateException("연결 끊김");
            }
            pushed.addAll(userIds);
        }

        @Override
        public boolean isOnline(Long userId) {
            return pushed.contains(userId);
        }
    }

    public static class Tickets implements StreamTicketPort {
        final Map<String, Long> issued = new HashMap<>();

        @Override
        public String issue(Long userId) {
            String t = "ticket-" + userId + "-abcdefghijk";
            issued.put(t, userId);
            return t;
        }

        @Override
        public Optional<Long> redeem(String ticket) {
            return Optional.ofNullable(issued.remove(ticket));
        }
    }
}
