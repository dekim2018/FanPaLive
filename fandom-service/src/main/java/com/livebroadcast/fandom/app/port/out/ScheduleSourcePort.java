package com.livebroadcast.fandom.app.port.out;

import java.util.List;

import com.livebroadcast.fandom.domain.schedule.ScheduleEntry;

/**
 * 경기 일정 공급원. 지금은 운영자가 올린 CSV 를 읽는 어댑터 하나뿐이고,
 * 외부 스포츠 데이터 API 를 쓰게 되면 이 포트의 구현만 하나 더 만들면 된다(서비스 코드는 그대로).
 */
public interface ScheduleSourcePort {

    /** 이 공급원의 이름(화면 표시용) */
    String name();

    List<ScheduleEntry> read(String leagueCode, String payload);
}
