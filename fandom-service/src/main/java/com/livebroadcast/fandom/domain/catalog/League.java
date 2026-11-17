package com.livebroadcast.fandom.domain.catalog;

/**
 * 종목 안의 리그 또는 투어.
 *
 * @param openForRequest 사용자가 팀/선수 등록을 신청할 수 있는 리그인지 (KBO 는 운영자가 넣으므로 false)
 */
public record League(Long id, String sportCode, String code, String name, String country, boolean openForRequest,
                     int sortOrder) {
}
