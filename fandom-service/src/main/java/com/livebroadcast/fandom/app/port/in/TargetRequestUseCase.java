package com.livebroadcast.fandom.app.port.in;

import java.util.List;

import com.livebroadcast.fandom.app.PageResult;
import com.livebroadcast.fandom.domain.target.FanTarget;
import com.livebroadcast.fandom.domain.target.RequestStatus;
import com.livebroadcast.fandom.domain.target.TargetRequest;

public interface TargetRequestUseCase {

    RequestOutcome request(RequestCommand command);

    TargetRequest vote(Long requestId, Long userId, String nickname);

    List<TargetRequest> mine(Long userId);

    PageResult<TargetRequest> list(RequestStatus status, String leagueCode, int page, int size);

    FanTarget approve(Long adminId, Long requestId);

    TargetRequest reject(Long adminId, Long requestId, String reason);

    FanTarget changeVisibility(Long targetId, boolean visible);

    record RequestCommand(Long userId, String nickname, String leagueCode, String name, String nameEn,
                          String shortName, String colorPrimary, String colorSecondary, String description,
                          String referenceUrl) {
    }

    /** merged = 같은 이름의 열린 신청이 있어서 새로 만들지 않고 표만 더했음 */
    record RequestOutcome(TargetRequest request, boolean merged) {
    }
}
