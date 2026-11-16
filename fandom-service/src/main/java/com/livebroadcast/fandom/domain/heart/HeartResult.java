package com.livebroadcast.fandom.domain.heart;

/**
 * 하트 요청 결과. 의심 하트여도 사용자에게는 똑같이 성공으로 보인다(우회 방법을 알려주지 않기 위해).
 */
public record HeartResult(int amount, int remainingToday, int remainingForTarget) {
}
