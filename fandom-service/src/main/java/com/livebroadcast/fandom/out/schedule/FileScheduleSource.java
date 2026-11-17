package com.livebroadcast.fandom.out.schedule;

import java.util.List;

import org.springframework.stereotype.Component;

import com.livebroadcast.fandom.app.port.out.ScheduleSourcePort;
import com.livebroadcast.fandom.domain.schedule.ScheduleEntry;
import com.livebroadcast.fandom.domain.schedule.ScheduleTextParser;

/**
 * 운영자가 붙여 넣거나 올린 CSV(또는 엑셀에서 복사한 탭 구분 글)를 일정으로 읽는다.
 * 형식: 날짜,시각,홈팀,원정팀,구장  예) 2026-04-01,18:30,두산 베어스,LG 트윈스,잠실
 */
@Component
public class FileScheduleSource implements ScheduleSourcePort {

    @Override
    public String name() {
        return "CSV 파일";
    }

    @Override
    public List<ScheduleEntry> read(String leagueCode, String payload) {
        return ScheduleTextParser.parse(payload);
    }
}
