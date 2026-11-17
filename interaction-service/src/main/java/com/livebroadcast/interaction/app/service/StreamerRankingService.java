package com.livebroadcast.interaction.app.service;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.livebroadcast.common.error.DomainException;
import com.livebroadcast.interaction.app.port.out.StreamerStatsPort;
import com.livebroadcast.interaction.domain.ranking.StreamerRank;

/**
 * 응원팀별 월간 스트리머 순위. "두산 편파방을 가장 많이 본 스트리머"처럼 팀 안에서만 비교한다.
 * 순시청자는 Redis HyperLogLog(스트리머마다 키 하나, 최대 12KB), 후원은 확정된 후원만 DB 에서 합친다.
 */
@Service
public class StreamerRankingService {

    static final int MAX_STREAMERS = 500;
    static final int TOP = 20;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final StreamerStatsPort stats;
    private final Clock clock;

    public StreamerRankingService(StreamerStatsPort stats, Clock clock) {
        this.stats = stats;
        this.clock = clock;
    }

    public StreamerRanking ranking(Long targetId, String month) {
        if (targetId == null) {
            throw DomainException.badRequest("TARGET_REQUIRED", "응원팀을 골라 주세요.");
        }
        YearMonth ym = parse(month);
        String key = ym.toString();
        Set<Long> ids = stats.streamers(targetId, key);
        Instant from = ym.atDay(1).atStartOfDay(KST).toInstant();
        Instant to = ym.plusMonths(1).atDay(1).atStartOfDay(KST).toInstant();
        Map<Long, Long> donations = stats.donations(targetId, from, to);
        Set<Long> all = new HashSet<>(ids);
        all.addAll(donations.keySet());
        List<Long> limited = all.stream().limit(MAX_STREAMERS).toList();
        Map<Long, Long> viewers = stats.uniqueViewers(targetId, key, limited);
        Map<Long, String> names = stats.nicknames(limited);
        List<StreamerRank> rows = new ArrayList<>();
        for (Long id : limited) {
            rows.add(new StreamerRank(0, id, names.getOrDefault(id, "스트리머"), viewers.getOrDefault(id, 0L),
                    donations.getOrDefault(id, 0L)));
        }
        rows.sort(Comparator.comparingLong(StreamerRank::score).reversed()
                .thenComparing(Comparator.comparingLong(StreamerRank::uniqueViewers).reversed())
                .thenComparingLong(StreamerRank::streamerId));
        List<StreamerRank> ranked = new ArrayList<>();
        for (int i = 0; i < Math.min(TOP, rows.size()); i++) {
            StreamerRank r = rows.get(i);
            ranked.add(new StreamerRank(i + 1, r.streamerId(), r.nickname(), r.uniqueViewers(), r.donationAmount()));
        }
        return new StreamerRanking(targetId, key, ranked);
    }

    private YearMonth parse(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.from(clock.instant().atZone(KST));
        }
        try {
            return YearMonth.parse(month.strip());
        } catch (DateTimeParseException e) {
            throw DomainException.badRequest("INVALID_MONTH", "월은 2026-11 형식으로 보내 주세요.");
        }
    }

    public record StreamerRanking(Long targetId, String month, List<StreamerRank> rows) {
    }
}
