package com.livebroadcast.broadcast.domain;

/**
 * 편파방 정보: 이 방송이 어느 팀(또는 선수) 편에서 하는 방송인지.
 * 응원 대상은 fandom-service 가 관리하고, 방송에는 화면에 바로 쓸 이름과 색을 복사해 둔다(조회 때 서비스 간 호출 없이 보여주기 위해).
 *
 * @param matchId 매치룸에 묶인 경기. 없으면 null
 */
public record FanSide(String leagueCode, Long targetId, String targetName, String targetShortName,
                      String targetColor, Long matchId) {

    public static final FanSide NONE = new FanSide(null, null, null, null, null, null);

    public boolean isPresent() {
        return targetId != null;
    }
}
