package com.livebroadcast.fandom.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.livebroadcast.fandom.app.port.in.MatchImportUseCase.ImportResult;
import com.livebroadcast.fandom.app.port.in.MatchImportUseCase.RowStatus;
import com.livebroadcast.fandom.app.service.MatchImportService;
import com.livebroadcast.fandom.out.schedule.FileScheduleSource;
import com.livebroadcast.fandom.support.ExtrasFakes;
import com.livebroadcast.fandom.support.Fakes;

class MatchImportServiceTest {

    final Fakes.Targets targets = new Fakes.Targets()
            .add(Fakes.team(9L, Fakes.KBO, "두산 베어스", "두산"))
            .add(Fakes.team(3L, Fakes.KBO, "LG 트윈스", "LG"));
    final ExtrasFakes.Matches matches = new ExtrasFakes.Matches();
    final MatchImportService service = new MatchImportService(new FileScheduleSource(), new Fakes.Catalog(), targets,
            matches, new FeatureGuard(Fakes.properties()), Fakes.properties(),
            Clock.fixed(Instant.parse("2026-03-01T00:00:00Z"), ZoneOffset.UTC));

    static final String CSV = """
            2026-04-01,18:30,두산 베어스,LG 트윈스,잠실
            2026-04-01,18:30,두산,LG,잠실
            2026-04-02,18:30,두산,한화,대전
            """;

    @Test
    void 미리보기는_저장하지_않고_새_경기_중복_오류를_나눠_보여준다() {
        ImportResult r = service.preview("KBO", CSV);

        assertThat(r.rows()).extracting(row -> row.status())
                .containsExactly(RowStatus.NEW, RowStatus.DUPLICATE, RowStatus.ERROR);
        assertThat(r.rows().get(2).message()).contains("한화");
        assertThat(matches.byId).isEmpty();
    }

    @Test
    void 등록하면_새_경기만_만들고_다시_올려도_중복으로_건너뛴다() {
        ImportResult first = service.run(1L, "KBO", CSV);
        ImportResult second = service.run(1L, "KBO", CSV);

        assertThat(first.created()).isEqualTo(1);
        assertThat(second.created()).isZero();
        assertThat(second.duplicates()).isEqualTo(2);
        assertThat(matches.byId).hasSize(1);
        // KST 18:30 은 UTC 09:30
        assertThat(matches.byId.values().iterator().next().getStartsAt())
                .isEqualTo(Instant.parse("2026-04-01T09:30:00Z"));
    }
}
