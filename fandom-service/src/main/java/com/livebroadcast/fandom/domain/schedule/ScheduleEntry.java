package com.livebroadcast.fandom.domain.schedule;

import java.time.LocalDateTime;

/**
 * 일정 공급원(파일, 나중에는 외부 API)에서 읽은 경기 한 줄. 팀은 아직 이름 그대로다(ID 로 바꾸기 전).
 *
 * @param line  원본의 몇 번째 줄인지(오류 안내용)
 * @param error 읽을 수 없는 줄이면 이유, 정상이면 null
 */
public record ScheduleEntry(int line, LocalDateTime startsAt, String homeName, String awayName, String venue,
                            String error) {

    public static ScheduleEntry invalid(int line, String error) {
        return new ScheduleEntry(line, null, null, null, null, error);
    }

    public boolean isValid() {
        return error == null;
    }
}
