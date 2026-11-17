package com.livebroadcast.fandom.out.persistence;

import static com.livebroadcast.fandom.out.persistence.JdbcSupport.instant;
import static com.livebroadcast.fandom.out.persistence.JdbcSupport.nullableLong;
import static com.livebroadcast.fandom.out.persistence.JdbcSupport.ts;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.livebroadcast.fandom.app.port.out.FanTargetRepositoryPort;
import com.livebroadcast.fandom.domain.target.FanTarget;
import com.livebroadcast.fandom.domain.target.TargetKind;
import com.livebroadcast.fandom.domain.target.TargetProfile;
import com.livebroadcast.fandom.domain.target.TargetStatus;

@Component
public class JdbcFanTargetRepository implements FanTargetRepositoryPort {

    static final RowMapper<FanTarget> MAPPER = (rs, i) -> new FanTarget(
            rs.getLong("id"),
            rs.getLong("league_id"),
            TargetKind.valueOf(rs.getString("kind")),
            new TargetProfile(rs.getString("name"), rs.getString("name_en"), rs.getString("short_name"),
                    rs.getString("color_primary"), rs.getString("color_secondary"), rs.getString("description")),
            TargetStatus.valueOf(rs.getString("status")),
            rs.getBoolean("official"),
            nullableLong(rs, "created_by"),
            instant(rs, "created_at"));

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;

    public JdbcFanTargetRepository(JdbcTemplate jdbc, NamedParameterJdbcTemplate named) {
        this.jdbc = jdbc;
        this.named = named;
    }

    @Override
    public FanTarget save(FanTarget t) {
        TargetProfile p = t.getProfile();
        if (t.getId() == null) {
            Long id = jdbc.queryForObject("""
                    INSERT INTO fan_targets (league_id, kind, name, name_en, normalized_name, short_name,
                                             color_primary, color_secondary, description, status, official,
                                             created_by, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    RETURNING id
                    """, Long.class, t.getLeagueId(), t.getKind().name(), p.name(), p.nameEn(), p.normalizedName(),
                    p.shortName(), p.colorPrimary(), p.colorSecondary(), p.description(), t.getStatus().name(),
                    t.isOfficial(), t.getCreatedBy(), ts(t.getCreatedAt()));
            // 통계 행을 함께 만들어 두면 이후 집계는 UPDATE 만 하면 된다.
            jdbc.update("INSERT INTO target_stats (target_id, updated_at) VALUES (?, NOW()) ON CONFLICT DO NOTHING",
                    id);
            return new FanTarget(id, t.getLeagueId(), t.getKind(), p, t.getStatus(), t.isOfficial(),
                    t.getCreatedBy(), t.getCreatedAt());
        }
        jdbc.update("""
                UPDATE fan_targets SET name = ?, name_en = ?, normalized_name = ?, short_name = ?, color_primary = ?,
                                       color_secondary = ?, description = ?, status = ?
                WHERE id = ?
                """, p.name(), p.nameEn(), p.normalizedName(), p.shortName(), p.colorPrimary(), p.colorSecondary(),
                p.description(), t.getStatus().name(), t.getId());
        return t;
    }

    @Override
    public Optional<FanTarget> findById(Long id) {
        return jdbc.query("SELECT * FROM fan_targets WHERE id = ?", MAPPER, id).stream().findFirst();
    }

    @Override
    public List<FanTarget> findByIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return named.query("SELECT * FROM fan_targets WHERE id IN (:ids)", Map.of("ids", ids), MAPPER);
    }

    @Override
    public List<FanTarget> findActiveByLeague(Long leagueId) {
        return jdbc.query("SELECT * FROM fan_targets WHERE league_id = ? AND status = 'ACTIVE' ORDER BY id",
                MAPPER, leagueId);
    }

    @Override
    public List<FanTarget> findAllActive() {
        return jdbc.query("SELECT * FROM fan_targets WHERE status = 'ACTIVE' ORDER BY id", MAPPER);
    }

    @Override
    public boolean existsByLeagueAndNormalizedName(Long leagueId, String normalizedName) {
        Boolean exists = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM fan_targets WHERE league_id = ? AND normalized_name = ?)",
                Boolean.class, leagueId, normalizedName);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public List<FanTarget> search(String keyword, int limit) {
        String like = "%" + keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return jdbc.query("""
                SELECT * FROM fan_targets
                WHERE status = 'ACTIVE'
                  AND (name ILIKE ? OR name_en ILIKE ? OR short_name ILIKE ?)
                ORDER BY official DESC, id
                LIMIT ?
                """, MAPPER, like, like, like, limit);
    }
}
