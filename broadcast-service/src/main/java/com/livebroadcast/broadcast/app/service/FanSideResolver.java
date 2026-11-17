package com.livebroadcast.broadcast.app.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.livebroadcast.broadcast.app.port.out.FandomQueryPort;
import com.livebroadcast.broadcast.app.port.out.FandomQueryPort.FandomUnavailableException;
import com.livebroadcast.broadcast.app.port.out.FandomQueryPort.MatchInfo;
import com.livebroadcast.broadcast.app.port.out.FandomQueryPort.TargetInfo;
import com.livebroadcast.broadcast.domain.BroadcastErrors;
import com.livebroadcast.broadcast.domain.FanSide;

/**
 * 방송에 붙일 응원팀과 경기를 확인한다.
 *
 * 장애 정책: fandom-service 가 응답하지 않으면 방송은 응원팀 없이(일반 방송으로) 만든다.
 * 부가 기능(팬덤) 장애 때문에 핵심 기능(방송 만들기, 송출)이 막히면 안 되기 때문이다.
 * 응답 화면은 응원팀이 비어 있는 것을 보고 "나중에 수정에서 지정"하도록 안내한다.
 */
@Component
public class FanSideResolver {

    private static final Logger log = LoggerFactory.getLogger(FanSideResolver.class);

    private final FandomQueryPort fandom;

    public FanSideResolver(FandomQueryPort fandom) {
        this.fandom = fandom;
    }

    public FanSide resolve(Long targetId, Long matchId) {
        if (targetId == null) {
            if (matchId != null) {
                throw BroadcastErrors.targetRequiredForMatch();
            }
            return FanSide.NONE;
        }
        try {
            TargetInfo target = fandom.findTarget(targetId)
                    .filter(TargetInfo::isActive)
                    .orElseThrow(BroadcastErrors::invalidFanTarget);
            Long validMatchId = null;
            if (matchId != null) {
                Optional<MatchInfo> match = fandom.findMatch(matchId);
                if (match.isEmpty() || !match.get().involves(targetId) || !match.get().isOpen()) {
                    throw BroadcastErrors.invalidMatch();
                }
                validMatchId = matchId;
            }
            return new FanSide(target.leagueCode(), target.id(), target.name(), target.shortName(),
                    target.colorPrimary(), validMatchId);
        } catch (FandomUnavailableException e) {
            log.warn("팬덤 서비스 응답 없음, 응원팀 없이 진행 targetId={} reason={}", targetId, e.getMessage());
            return FanSide.NONE;
        }
    }
}
