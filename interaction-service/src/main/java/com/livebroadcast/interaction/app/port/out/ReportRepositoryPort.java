package com.livebroadcast.interaction.app.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.livebroadcast.interaction.app.PageResult;
import com.livebroadcast.interaction.domain.report.Report;
import com.livebroadcast.interaction.domain.report.ReportStatus;
import com.livebroadcast.interaction.domain.report.ReportType;

public interface ReportRepositoryPort {

    /** 이미 같은 사람이 같은 대상을 신고했으면 empty */
    Optional<Report> insert(Report report);

    Optional<Report> findById(Long id);

    PageResult<Report> findByStatus(ReportStatus status, int page, int size);

    /** 같은 대상의 열린 신고 수(관리자 화면 정렬용) */
    long countOpen(ReportType type, Long refId);

    /** 같은 대상의 열린 신고를 한 번에 닫는다. 닫은 건수 */
    int resolveAll(ReportType type, Long refId, ReportStatus status, Long adminId, Instant now);

    List<Report> findOpenByTarget(ReportType type, Long refId);
}
