package com.livebroadcast.broadcast.app.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livebroadcast.broadcast.app.BroadcastFilter;
import com.livebroadcast.broadcast.app.MediaProperties;
import com.livebroadcast.broadcast.app.PageResult;
import com.livebroadcast.broadcast.app.port.in.BroadcastCommandUseCase;
import com.livebroadcast.broadcast.app.port.in.BroadcastQueryUseCase;
import com.livebroadcast.broadcast.app.port.in.LiveStreamUseCase;
import com.livebroadcast.broadcast.app.port.out.BroadcastRepositoryPort;
import com.livebroadcast.broadcast.app.port.out.DomainEventPublisherPort;
import com.livebroadcast.broadcast.domain.Broadcast;
import com.livebroadcast.broadcast.domain.BroadcastErrors;
import com.livebroadcast.broadcast.domain.BroadcastStatus;
import com.livebroadcast.broadcast.domain.FanSide;
import com.livebroadcast.broadcast.domain.SourceType;
import com.livebroadcast.broadcast.domain.event.BroadcastEnded;

@Service
public class BroadcastService implements BroadcastCommandUseCase, BroadcastQueryUseCase, LiveStreamUseCase {

    private static final int MAX_PAGE_SIZE = 50;

    private final BroadcastRepositoryPort broadcastRepository;
    private final DomainEventPublisherPort events;
    private final PublishTokenGenerator tokenGenerator;
    private final MediaProperties mediaProperties;
    private final FanSideResolver fanSideResolver;
    private final Clock clock;

    public BroadcastService(BroadcastRepositoryPort broadcastRepository, DomainEventPublisherPort events,
                            PublishTokenGenerator tokenGenerator, MediaProperties mediaProperties,
                            FanSideResolver fanSideResolver, Clock clock) {
        this.broadcastRepository = broadcastRepository;
        this.events = events;
        this.tokenGenerator = tokenGenerator;
        this.mediaProperties = mediaProperties;
        this.fanSideResolver = fanSideResolver;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Broadcast create(CreateCommand command) {
        FanSide side = fanSideResolver.resolve(command.targetId(), command.matchId());
        Broadcast broadcast = Broadcast.create(command.ownerId(), command.ownerNickname(), command.title(),
                command.description(), side, clock.instant());
        return broadcastRepository.save(broadcast);
    }

    @Override
    @Transactional
    public Broadcast update(UpdateCommand command) {
        Broadcast broadcast = lock(command.broadcastId());
        // 소유자 확인을 외부 호출보다 먼저 한다(남의 방송이면 fandom-service 를 부르지 않고 바로 403).
        broadcast.ensureOwner(command.requesterId());
        FanSide side = command.targetId() == null && !command.clearTarget()
                ? broadcast.getSide()
                : fanSideResolver.resolve(command.targetId(), command.matchId());
        broadcast.updateInfo(command.requesterId(), command.title(), command.description(), side, clock.instant());
        return broadcastRepository.save(broadcast);
    }

    @Override
    @Transactional
    public void delete(Long broadcastId, Long requesterId) {
        Broadcast broadcast = lock(broadcastId);
        broadcast.ensureDeletable(requesterId);
        broadcastRepository.delete(broadcastId);
    }

    @Override
    @Transactional(readOnly = true)
    public Broadcast get(Long broadcastId) {
        return broadcastRepository.findById(broadcastId).orElseThrow(BroadcastErrors::notFound);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Broadcast> list(BroadcastStatus status, int page, int size) {
        return list(BroadcastFilter.ofStatus(status), page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Broadcast> list(BroadcastFilter filter, int page, int size) {
        return broadcastRepository.findAll(filter, Math.max(page, 0), clampSize(size));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Broadcast> listByOwner(Long ownerId, int page, int size) {
        return broadcastRepository.findByOwner(ownerId, Math.max(page, 0), clampSize(size));
    }

    @Override
    @Transactional
    public LiveSession start(Long broadcastId, Long requesterId, SourceType sourceType) {
        Broadcast broadcast = lock(broadcastId);
        broadcast.startLive(requesterId, sourceType, tokenGenerator.generate(), clock.instant());
        Broadcast saved = broadcastRepository.save(broadcast);
        return toSession(saved);
    }

    @Override
    @Transactional
    public Broadcast end(Long broadcastId, Long requesterId) {
        Broadcast broadcast = lock(broadcastId);
        Instant now = clock.instant();
        broadcast.endLive(requesterId, now);
        Broadcast saved = broadcastRepository.save(broadcast);
        // 녹화 마무리, 남은 송출 연결 끊기는 BroadcastEnded 리스너가 처리한다.
        events.publish(new BroadcastEnded(saved.getId(), saved.mediaPath(), now));
        return saved;
    }

    @Override
    @Transactional
    public boolean moderate(Long broadcastId, String reason) {
        Broadcast broadcast = lock(broadcastId);
        Instant now = clock.instant();
        boolean ended = broadcast.moderate(reason, now);
        Broadcast saved = broadcastRepository.save(broadcast);
        if (ended) {
            // 사용자가 종료한 것과 같은 흐름: 녹화 마무리, 송출 연결 끊기
            events.publish(new BroadcastEnded(saved.getId(), saved.mediaPath(), now));
        }
        return ended;
    }

    @Override
    @Transactional(readOnly = true)
    public LiveSession getSession(Long broadcastId, Long requesterId) {
        Broadcast broadcast = get(broadcastId);
        broadcast.ensureOwner(requesterId);
        broadcast.ensureOnAir();
        return toSession(broadcast);
    }

    private LiveSession toSession(Broadcast broadcast) {
        return new LiveSession(broadcast, mediaProperties.whipUrl(broadcast.mediaPath()), broadcast.getPublishToken());
    }

    private Broadcast lock(Long broadcastId) {
        return broadcastRepository.findByIdForUpdate(broadcastId).orElseThrow(BroadcastErrors::notFound);
    }

    private int clampSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
