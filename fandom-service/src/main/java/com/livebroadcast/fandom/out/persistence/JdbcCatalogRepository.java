package com.livebroadcast.fandom.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import com.livebroadcast.fandom.app.port.out.CatalogRepositoryPort;
import com.livebroadcast.fandom.domain.catalog.League;
import com.livebroadcast.fandom.domain.catalog.Sport;
import com.livebroadcast.fandom.domain.catalog.SportKind;

@Component
public class JdbcCatalogRepository implements CatalogRepositoryPort {

    private static final RowMapper<League> LEAGUE = (rs, i) -> new League(rs.getLong("id"),
            rs.getString("sport_code"), rs.getString("code"), rs.getString("name"), rs.getString("country"),
            rs.getBoolean("open_for_request"), rs.getInt("sort_order"));

    private final JdbcTemplate jdbc;

    public JdbcCatalogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Sport> findSports() {
        return jdbc.query("SELECT code, name, kind, sort_order FROM sports ORDER BY sort_order, code",
                (rs, i) -> new Sport(rs.getString("code"), rs.getString("name"),
                        SportKind.valueOf(rs.getString("kind")), rs.getInt("sort_order")));
    }

    @Override
    public List<League> findLeagues() {
        return jdbc.query("SELECT * FROM leagues ORDER BY sort_order, id", LEAGUE);
    }

    @Override
    public Optional<League> findLeagueByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return jdbc.query("SELECT * FROM leagues WHERE code = ?", LEAGUE, code.strip().toUpperCase())
                .stream().findFirst();
    }

    @Override
    public Optional<League> findLeagueById(Long id) {
        return jdbc.query("SELECT * FROM leagues WHERE id = ?", LEAGUE, id).stream().findFirst();
    }
}
