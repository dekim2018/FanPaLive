package com.livebroadcast.fandom.out.redis;

import java.time.LocalDate;

/** 팬덤 서비스가 쓰는 Redis 키. 다른 서비스와 겹치지 않도록 fanpa: 접두사를 쓴다. */
final class RedisKeys {

    private RedisKeys() {
    }

    static String heartDaily(LocalDate date, Long userId) {
        return "fanpa:heart:" + date + ":u:" + userId;
    }

    static String heartDailyTarget(LocalDate date, Long userId, Long targetId) {
        return "fanpa:heart:" + date + ":u:" + userId + ":t:" + targetId;
    }

    static String abuseDevice(LocalDate date, Long targetId, String deviceKey) {
        return "fanpa:abuse:" + date + ":t:" + targetId + ":d:" + deviceKey;
    }

    static String abuseIp(LocalDate date, Long targetId, String ipKey) {
        return "fanpa:abuse:" + date + ":t:" + targetId + ":ip:" + ipKey;
    }

    static String ranking(Long leagueId, String periodKey) {
        return "fanpa:rank:" + leagueId + ":" + periodKey;
    }

    static String matchGauge(Long matchId) {
        return "fanpa:match:" + matchId + ":gauge";
    }

    static String streamTicket(String ticket) {
        return "fanpa:sse-ticket:" + ticket;
    }
}
