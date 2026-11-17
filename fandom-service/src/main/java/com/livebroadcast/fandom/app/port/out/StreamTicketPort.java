package com.livebroadcast.fandom.app.port.out;

import java.util.Optional;

/**
 * 실시간 알림 구독용 1회용 티켓. 브라우저 EventSource 는 Authorization 헤더를 못 보내서,
 * 로그인 토큰을 주소에 싣는 대신 60초짜리 1회용 티켓을 발급해 주소에 넣는다.
 */
public interface StreamTicketPort {

    String issue(Long userId);

    /** 티켓을 꺼내면서 지운다(재사용 불가) */
    Optional<Long> redeem(String ticket);
}
