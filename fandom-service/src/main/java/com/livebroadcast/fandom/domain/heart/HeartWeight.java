package com.livebroadcast.fandom.domain.heart;

/**
 * 하트 1개가 랭킹에 반영되는 점수.
 *
 * @param pointsPerHeart 기본 10점, 신규 계정은 5점, 의심 하트는 0점
 * @param suspicious     몰아주기가 의심되는 하트. 기록은 남기지만 랭킹에서는 뺀다. 사용자에게는 알리지 않는다.
 * @param reason         판단 근거(운영 확인용)
 */
public record HeartWeight(int pointsPerHeart, boolean suspicious, String reason) {
}
