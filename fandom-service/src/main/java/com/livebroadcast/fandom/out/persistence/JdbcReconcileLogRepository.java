package com.livebroadcast.fandom.out.persistence;

import static com.livebroadcast.fandom.out.persistence.JdbcSupport.date;
import static com.livebroadcast.fandom.out.persistence.JdbcSupport.ts;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.livebroadcast.fandom.app.port.out.ReconcileLogPort;

@Component
public class JdbcReconcileLogRepository implements ReconcileLogPort {

    private final JdbcTemplate jdbc;

    public JdbcReconcileLogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void record(LocalDate statDate, Long targetId, long expected, long actual, Instant at) {
        jdbc.update("INSERT INTO reconcile_logs (stat_date, target_id, expected, actual, created_at) VALUES (?, ?, ?, ?, ?)",
                date(statDate), targetId, expected, actual, ts(at));
    }
}
