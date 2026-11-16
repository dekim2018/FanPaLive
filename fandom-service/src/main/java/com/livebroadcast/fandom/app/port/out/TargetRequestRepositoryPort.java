package com.livebroadcast.fandom.app.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.livebroadcast.fandom.app.PageResult;
import com.livebroadcast.fandom.domain.target.RequestStatus;
import com.livebroadcast.fandom.domain.target.TargetRequest;

public interface TargetRequestRepositoryPort {

    TargetRequest save(TargetRequest request);

    Optional<TargetRequest> findById(Long id);

    /** 승인/반려/투표가 동시에 들어와도 하나씩 처리되도록 행 잠금으로 조회한다. */
    Optional<TargetRequest> findByIdForUpdate(Long id);

    Optional<TargetRequest> findOpenByLeagueAndName(Long leagueId, String normalizedName);

    /** 표가 많은 순, 같으면 먼저 신청한 순 */
    PageResult<TargetRequest> findByStatus(RequestStatus status, Long leagueId, int page, int size);

    List<TargetRequest> findByRequester(Long requesterId);

    /** 같은 사람의 두 번째 투표는 false (PK 로 막는다) */
    boolean insertVote(Long requestId, Long userId, String nickname, Instant now);

    boolean hasVoted(Long requestId, Long userId);

    /** 신청자와 "나도 원해요"를 누른 사람. 승인되면 자동으로 관심 팬이 된다. */
    List<Voter> findVoters(Long requestId);

    record Voter(Long userId, String nickname) {
    }
}
