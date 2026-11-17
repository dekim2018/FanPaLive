package com.livebroadcast.fandom.out.persistence;

import static com.livebroadcast.fandom.out.persistence.JdbcSupport.ts;

import java.time.Instant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.livebroadcast.fandom.app.port.out.DonationContributionPort;
import com.livebroadcast.fandom.domain.donation.DonationEvent;

@Component
public class JdbcDonationContributionRepository implements DonationContributionPort {

    private final JdbcTemplate jdbc;

    public JdbcDonationContributionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean insertIfAbsent(DonationEvent e) {
        int inserted = jdbc.update("""
                INSERT INTO donation_contributions (event_id, target_id, donor_id, donor_nickname, amount,
                                                    approved_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (event_id) DO NOTHING
                """, e.eventId(), e.targetId(), e.donorId(), e.donorNickname() == null ? "" : e.donorNickname(),
                e.amount(), ts(e.approvedAt()), ts(Instant.now()));
        return inserted == 1;
    }
}
