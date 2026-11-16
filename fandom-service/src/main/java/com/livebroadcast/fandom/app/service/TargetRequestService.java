package com.livebroadcast.fandom.app.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livebroadcast.fandom.app.FeatureGuard;
import com.livebroadcast.fandom.app.PageResult;
import com.livebroadcast.fandom.app.port.in.TargetRequestUseCase;
import com.livebroadcast.fandom.app.port.out.CatalogRepositoryPort;
import com.livebroadcast.fandom.app.port.out.FanTargetRepositoryPort;
import com.livebroadcast.fandom.app.port.out.TargetRequestRepositoryPort;
import com.livebroadcast.fandom.domain.catalog.League;
import com.livebroadcast.fandom.domain.catalog.Sport;
import com.livebroadcast.fandom.domain.catalog.SportKind;
import com.livebroadcast.fandom.domain.target.FanTarget;
import com.livebroadcast.fandom.domain.target.RequestStatus;
import com.livebroadcast.fandom.domain.target.TargetErrors;
import com.livebroadcast.fandom.domain.target.TargetKind;
import com.livebroadcast.fandom.domain.target.TargetProfile;
import com.livebroadcast.fandom.domain.target.TargetRequest;

@Service
public class TargetRequestService implements TargetRequestUseCase {

    private static final int MAX_PAGE_SIZE = 50;

    private final TargetRequestRepositoryPort requests;
    private final FanTargetRepositoryPort targets;
    private final CatalogRepositoryPort catalog;
    private final FanService fanService;
    private final TargetLookup lookup;
    private final FeatureGuard features;
    private final Clock clock;

    public TargetRequestService(TargetRequestRepositoryPort requests, FanTargetRepositoryPort targets,
                                CatalogRepositoryPort catalog, FanService fanService, TargetLookup lookup,
                                FeatureGuard features, Clock clock) {
        this.requests = requests;
        this.targets = targets;
        this.catalog = catalog;
        this.fanService = fanService;
        this.lookup = lookup;
        this.features = features;
        this.clock = clock;
    }

    /**
     * 신청 흐름: 리그가 신청을 받는지 확인 -> 이미 있는 대상인지 확인 -> 같은 이름의 열린 신청이 있으면 표만 더하기 -> 새 신청.
     * 팀 종목 리그는 구단, 개인 종목 리그는 선수로 자동 분류한다.
     */
    @Override
    @Transactional
    public RequestOutcome request(RequestCommand command) {
        features.targetRequests();
        League league = catalog.findLeagueByCode(command.leagueCode()).orElseThrow(TargetErrors::leagueNotFound);
        if (!league.openForRequest()) {
            throw TargetErrors.leagueClosed();
        }
        TargetKind kind = kindOf(league);
        TargetProfile profile = new TargetProfile(command.name(), command.nameEn(), command.shortName(),
                command.colorPrimary(), command.colorSecondary(), command.description());
        if (targets.existsByLeagueAndNormalizedName(league.id(), profile.normalizedName())) {
            throw TargetErrors.alreadyExists();
        }
        Optional<TargetRequest> open = requests.findOpenByLeagueAndName(league.id(), profile.normalizedName());
        if (open.isPresent()) {
            TargetRequest existing = open.get();
            if (!requests.hasVoted(existing.getId(), command.userId())) {
                return new RequestOutcome(vote(existing.getId(), command.userId(), command.nickname()), true);
            }
            return new RequestOutcome(existing, true);
        }
        Instant now = clock.instant();
        TargetRequest saved = requests.save(TargetRequest.open(league.id(), kind, profile, command.referenceUrl(),
                command.userId(), command.nickname(), now));
        requests.insertVote(saved.getId(), command.userId(), command.nickname(), now);
        return new RequestOutcome(saved, false);
    }

    @Override
    @Transactional
    public TargetRequest vote(Long requestId, Long userId, String nickname) {
        features.targetRequests();
        TargetRequest request = requests.findByIdForUpdate(requestId).orElseThrow(TargetErrors::requestNotFound);
        request.ensureOpen();
        if (!requests.insertVote(requestId, userId, nickname, clock.instant())) {
            throw TargetErrors.alreadyVoted();
        }
        request.addVote();
        return requests.save(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TargetRequest> mine(Long userId) {
        return requests.findByRequester(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<TargetRequest> list(RequestStatus status, String leagueCode, int page, int size) {
        Long leagueId = null;
        if (leagueCode != null && !leagueCode.isBlank()) {
            leagueId = catalog.findLeagueByCode(leagueCode).map(League::id).orElseThrow(TargetErrors::leagueNotFound);
        }
        int safeSize = size <= 0 ? 20 : Math.min(size, MAX_PAGE_SIZE);
        return requests.findByStatus(status == null ? RequestStatus.PENDING : status, leagueId, Math.max(page, 0),
                safeSize);
    }

    /** 승인하면 대상을 만들고, 신청자와 투표자를 관심 팬으로 등록한다(첫 팬 0명으로 시작하지 않도록). */
    @Override
    @Transactional
    public FanTarget approve(Long adminId, Long requestId) {
        TargetRequest request = requests.findByIdForUpdate(requestId).orElseThrow(TargetErrors::requestNotFound);
        request.ensureOpen();
        if (targets.existsByLeagueAndNormalizedName(request.getLeagueId(), request.normalizedName())) {
            throw TargetErrors.alreadyExists();
        }
        Instant now = clock.instant();
        FanTarget target = targets.save(FanTarget.fromRequest(request, now));
        request.approve(adminId, target.getId(), now);
        requests.save(request);
        fanService.followAll(target.getId(), target.getLeagueId(), requests.findVoters(requestId).stream()
                .map(v -> new FanService.Follower(v.userId(), v.nickname()))
                .toList());
        return target;
    }

    @Override
    @Transactional
    public TargetRequest reject(Long adminId, Long requestId, String reason) {
        TargetRequest request = requests.findByIdForUpdate(requestId).orElseThrow(TargetErrors::requestNotFound);
        request.reject(adminId, reason, clock.instant());
        return requests.save(request);
    }

    @Override
    @Transactional
    public FanTarget changeVisibility(Long targetId, boolean visible) {
        FanTarget target = targets.findById(targetId).orElseThrow(TargetErrors::notFound);
        if (visible) {
            target.show();
        } else {
            target.hide();
        }
        FanTarget saved = targets.save(target);
        lookup.evict(targetId);
        return saved;
    }

    private TargetKind kindOf(League league) {
        SportKind kind = catalog.findSports().stream()
                .filter(s -> s.code().equals(league.sportCode()))
                .map(Sport::kind)
                .findFirst()
                .orElse(SportKind.TEAM);
        return kind == SportKind.TEAM ? TargetKind.TEAM : TargetKind.PLAYER;
    }
}
