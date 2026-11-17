package com.livebroadcast.interaction.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.livebroadcast.interaction.app.port.out.StreamerStatsPort;
import com.livebroadcast.interaction.app.service.StreamerRankingService.StreamerRanking;

class StreamerRankingServiceTest {

    final Stats stats = new Stats();
    // 2026-10-31T16:00Z 는 KST 11월 1일 01시
    final StreamerRankingService service = new StreamerRankingService(stats,
            Clock.fixed(Instant.parse("2026-10-31T16:00:00Z"), ZoneOffset.UTC));

    @Test
    void 이번_달은_KST_기준이고_순시청자와_후원을_합친_점수로_정렬한다() {
        stats.viewers.put(1L, 120L);
        stats.viewers.put(2L, 100L);
        stats.donations.put(2L, 50_000L);
        stats.donations.put(3L, 5_000L);
        stats.names.put(1L, "곰돌");
        stats.names.put(2L, "잠실");

        StreamerRanking r = service.ranking(9L, null);

        assertThat(r.month()).isEqualTo("2026-11");
        assertThat(r.rows()).extracting(row -> row.streamerId()).containsExactly(2L, 1L, 3L);
        assertThat(r.rows().get(0).score()).isEqualTo(150);
        assertThat(r.rows().get(2).nickname()).isEqualTo("스트리머");
    }

    @Test
    void 월_형식이_틀리면_알려준다() {
        assertThatThrownBy(() -> service.ranking(9L, "11월")).hasMessageContaining("2026-11");
    }

    static class Stats implements StreamerStatsPort {
        final Map<Long, Long> viewers = new HashMap<>();
        final Map<Long, Long> donations = new HashMap<>();
        final Map<Long, String> names = new HashMap<>();

        @Override
        public void recordView(Long targetId, String monthKey, Long streamerId, String nickname, String viewerKey) {
        }

        @Override
        public Set<Long> streamers(Long targetId, String monthKey) {
            return new HashSet<>(viewers.keySet());
        }

        @Override
        public Map<Long, Long> uniqueViewers(Long targetId, String monthKey, Collection<Long> ids) {
            return viewers;
        }

        @Override
        public Map<Long, String> nicknames(Collection<Long> ids) {
            return names;
        }

        @Override
        public Map<Long, Long> donations(Long targetId, Instant from, Instant to) {
            return donations;
        }
    }
}
