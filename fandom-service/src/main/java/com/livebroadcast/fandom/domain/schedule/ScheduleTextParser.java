package com.livebroadcast.fandom.domain.schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV 나 엑셀에서 복사한 표(탭 구분)로 된 경기 일정을 읽는다.
 * 한 줄 형식: 날짜, 시작 시각, 홈팀, 원정팀, 경기장(선택)
 * 예: 2026-11-20,18:30,두산 베어스,LG 트윈스,잠실
 * 첫 줄이 "날짜" 또는 "date" 로 시작하면 머리글로 보고 건너뛴다. # 으로 시작하는 줄은 주석.
 */
public final class ScheduleTextParser {

    public static final int MAX_LINES = 500;

    private ScheduleTextParser() {
    }

    public static List<ScheduleEntry> parse(String text) {
        List<ScheduleEntry> entries = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return entries;
        }
        String[] lines = text.replace("\r", "").split("\n");
        for (int i = 0; i < lines.length; i++) {
            String raw = lines[i].strip();
            int lineNo = i + 1;
            if (raw.isEmpty() || raw.startsWith("#") || (i == 0 && isHeader(raw))) {
                continue;
            }
            if (entries.size() >= MAX_LINES) {
                entries.add(ScheduleEntry.invalid(lineNo, "한 번에 " + MAX_LINES + "경기까지 올릴 수 있습니다."));
                break;
            }
            entries.add(parseLine(lineNo, raw));
        }
        return entries;
    }

    private static boolean isHeader(String line) {
        String lower = line.toLowerCase();
        return lower.startsWith("날짜") || lower.startsWith("date");
    }

    static ScheduleEntry parseLine(int lineNo, String line) {
        String[] cols = line.contains("\t") ? line.split("\t", -1) : line.split(",", -1);
        if (cols.length < 4) {
            return ScheduleEntry.invalid(lineNo, "날짜, 시각, 홈팀, 원정팀 네 칸이 필요합니다.");
        }
        LocalDate date;
        LocalTime time;
        try {
            date = LocalDate.parse(cols[0].strip().replace('.', '-').replace('/', '-'));
        } catch (DateTimeParseException e) {
            return ScheduleEntry.invalid(lineNo, "날짜 형식이 올바르지 않습니다(예: 2026-11-20).");
        }
        try {
            String t = cols[1].strip();
            time = LocalTime.parse(t.length() == 4 ? "0" + t : t);
        } catch (DateTimeParseException e) {
            return ScheduleEntry.invalid(lineNo, "시각 형식이 올바르지 않습니다(예: 18:30).");
        }
        String home = cols[2].strip();
        String away = cols[3].strip();
        if (home.isEmpty() || away.isEmpty()) {
            return ScheduleEntry.invalid(lineNo, "홈팀과 원정팀 이름을 적어 주세요.");
        }
        if (home.equalsIgnoreCase(away)) {
            return ScheduleEntry.invalid(lineNo, "홈팀과 원정팀이 같습니다.");
        }
        String venue = cols.length > 4 ? cols[4].strip() : "";
        return new ScheduleEntry(lineNo, LocalDateTime.of(date, time), home, away, venue, null);
    }
}
