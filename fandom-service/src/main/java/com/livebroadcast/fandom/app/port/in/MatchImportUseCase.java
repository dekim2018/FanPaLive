package com.livebroadcast.fandom.app.port.in;

import java.time.Instant;
import java.util.List;

public interface MatchImportUseCase {

    /** 저장하지 않고 어떻게 들어갈지만 보여준다 */
    ImportResult preview(String leagueCode, String payload);

    ImportResult run(Long adminId, String leagueCode, String payload);

    enum RowStatus { NEW, DUPLICATE, ERROR, CREATED }

    record ImportRow(int line, Instant startsAt, String home, String away, String venue, RowStatus status,
                     String message) {
    }

    record ImportResult(String source, int created, int duplicates, int errors, List<ImportRow> rows) {
    }
}
