package com.livebroadcast.interaction.domain.donation;

/**
 * 후원 종류.
 * <ul>
 *     <li>BROADCAST: 방송자 후원. 방송이 편파방이면 그 응원팀의 랭킹 점수에도 들어간다(편파방 후원).</li>
 *     <li>FAN_TARGET: 구단/선수 응원금. 실제로 구단에 전달되지 않는 가상 기금이고 랭킹 점수에만 쓰인다.</li>
 * </ul>
 */
public enum DonationTargetType {
    BROADCAST,
    FAN_TARGET
}
