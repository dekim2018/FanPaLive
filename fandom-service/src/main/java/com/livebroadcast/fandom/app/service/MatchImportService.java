package com.livebroadcast.fandom.app.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livebroadcast.fandom.app.FandomProperties;
import com.livebroadcast.fandom.app.FeatureGuard;
import com.livebroadcast.fandom.app.port.in.MatchImportUseCase;
import com.livebroadcast.fandom.app.port.out.CatalogRepositoryPort;
import com.livebroadcast.fandom.app.port.out.FanTargetRepositoryPort;
import com.livebroadcast.fandom.app.port.out.MatchRepositoryPort;
import com.livebroadcast.fandom.app.port.out.ScheduleSourcePort;
import com.livebroadcast.fandom.domain.catalog.League;
import com.livebroadcast.fandom.domain.match.Match;
import com.livebroadcast.fandom.domain.schedule.ScheduleEntry;
import com.livebroadcast.fandom.domain.target.FanTarget;
import com.livebroadcast.fandom.domain.target.NameNormalizer;
import com.livebroadcast.fandom.domain.target.TargetErrors;

/**
 * 경기 일정 일괄 등록. 공급원(ScheduleSourcePort)이 읽은 줄을 팀 이름으로 매칭하고,
 * 이미 있는 경기(같은 리그, 같은 두 팀, 같은 시작 시각)는 건너뛴다. 미리보기와 실제 등록이 같은 판단을 쓴다.
 */
@Service
public class MatchImportService implements MatchImportUseCase {

    private final ScheduleSourcePort source;
    private final CatalogRepositoryPort catalog;
    private final FanTargetRepositoryPort targets;
    private final MatchRepositoryPort matches;
    private final FeatureGuard features;
    private final ZoneId zone;
    private final Clock clock;

    public MatchImportService(ScheduleSourcePort source, CatalogRepositoryPort catalog,
                              FanTargetRepositoryPort targets, MatchRepositoryPort matches, FeatureGuard features,
                              FandomProperties properties, Clock clock) {
        this.source = source;
        this.catalog = catalog;
        this.targets = targets;
        this.matches = matches;
        this.features = features;
        this.zone = properties.periodKeys().zone();
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ImportResult preview(String leagueCode, String payload) {
        return plan(leagueCode, payload, null);
    }

    @Override
    @Transactional
    public ImportResult run(Long adminId, String leagueCode, String payload) {
        return plan(leagueCode, payload, adminId);
    }

    private ImportResult plan(String leagueCode, String payload, Long adminId) {
        features.matches();
        League league = catalog.findLeagueByCode(leagueCode).orElseThrow(TargetErrors::leagueNotFound);
        Map<String, FanTarget> byName = nameIndex(targets.findActiveByLeague(league.id()));
        List<ScheduleEntry> entries = source.read(leagueCode, payload);
        Map<LocalDate, Set<String>> existing = new HashMap<>();
        Set<String> seenInFile = new HashSet<>();
        List<ImportRow> rows = new ArrayList<>();
        int created = 0;
        int duplicates = 0;
        int errors = 0;
        Instant now = clock.instant();
        for (ScheduleEntry e : entries) {
            if (!e.isValid()) {
                rows.add(new ImportRow(e.line(), null, null, null, null, RowStatus.ERROR, e.error()));
                errors++;
                continue;
            }
            Instant startsAt = e.startsAt().atZone(zone).toInstant();
            FanTarget home = byName.get(NameNormalizer.normalize(e.homeName()));
            FanTarget away = byName.get(NameNormalizer.normalize(e.awayName()));
            if (home == null || away == null) {
                String missing = home == null ? e.homeName() : e.awayName();
                rows.add(new ImportRow(e.line(), startsAt, e.homeName(), e.awayName(), e.venue(), RowStatus.ERROR,
                        "'" + missing + "' 팀을 " + league.name() + "에서 찾을 수 없습니다."));
                errors++;
                continue;
            }
            if (home.getId().equals(away.getId())) {
                rows.add(new ImportRow(e.line(), startsAt, home.getProfile().name(), away.getProfile().name(),
                        e.venue(), RowStatus.ERROR, "홈팀과 원정팀이 같습니다."));
                errors++;
                continue;
            }
            String slot = slot(home.getId(), away.getId(), startsAt);
            LocalDate day = e.startsAt().toLocalDate();
            Set<String> dayMatches = existing.computeIfAbsent(day, d -> existingSlots(league.id(), d));
            if (dayMatches.contains(slot) || !seenInFile.add(slot)) {
                rows.add(new ImportRow(e.line(), startsAt, home.getProfile().name(), away.getProfile().name(),
                        e.venue(), RowStatus.DUPLICATE, "이미 등록된 경기입니다."));
                duplicates++;
                continue;
            }
            if (adminId == null) {
                rows.add(new ImportRow(e.line(), startsAt, home.getProfile().name(), away.getProfile().name(),
                        e.venue(), RowStatus.NEW, null));
                continue;
            }
            matches.save(Match.schedule(league.id(), home.getId(), away.getId(), startsAt, e.venue(), adminId, now));
            dayMatches.add(slot);
            rows.add(new ImportRow(e.line(), startsAt, home.getProfile().name(), away.getProfile().name(), e.venue(),
                    RowStatus.CREATED, null));
            created++;
        }
        return new ImportResult(source.name(), created, duplicates, errors, rows);
    }

    /** 정식 이름과 약칭 모두로 찾을 수 있게 한다(예: "두산 베어스", "두산") */
    static Map<String, FanTarget> nameIndex(List<FanTarget> list) {
        Map<String, FanTarget> index = new HashMap<>();
        for (FanTarget t : list) {
            index.put(NameNormalizer.normalize(t.getProfile().name()), t);
            if (t.getProfile().nameEn() != null && !t.getProfile().nameEn().isBlank()) {
                index.putIfAbsent(NameNormalizer.normalize(t.getProfile().nameEn()), t);
            }
            if (t.getProfile().shortName() != null && !t.getProfile().shortName().isBlank()) {
                index.putIfAbsent(NameNormalizer.normalize(t.getProfile().shortName()), t);
            }
        }
        return index;
    }

    private Set<String> existingSlots(Long leagueId, LocalDate day) {
        Instant from = day.atStartOfDay(zone).toInstant();
        Instant to = day.plusDays(1).atStartOfDay(zone).toInstant();
        Set<String> slots = new HashSet<>();
        for (Match m : matches.findByStartsAtBetween(from, to, leagueId)) {
            slots.add(slot(m.getHomeTargetId(), m.getAwayTargetId(), m.getStartsAt()));
        }
        return slots;
    }

    private static String slot(Long home, Long away, Instant startsAt) {
        return home + ":" + away + ":" + startsAt.getEpochSecond();
    }
}
