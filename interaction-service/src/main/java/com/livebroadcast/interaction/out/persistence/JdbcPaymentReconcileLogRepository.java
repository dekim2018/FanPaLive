package com.livebroadcast.interaction.out.persistence;

import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.livebroadcast.interaction.app.port.out.PaymentReconcileLogPort;

@Component
public class JdbcPaymentReconcileLogRepository implements PaymentReconcileLogPort {

    private final JdbcTemplate jdbc;

    public JdbcPaymentReconcileLogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void record(String orderId, String issue, String detail, Instant at) {
        String safeDetail = detail == null ? null : detail.substring(0, Math.min(detail.length(), 200));
        jdbc.update("INSERT INTO payment_reconcile_logs (order_id, issue, detail, created_at) VALUES (?, ?, ?, ?)",
                orderId, issue, safeDetail, Timestamp.from(at));
    }
}
