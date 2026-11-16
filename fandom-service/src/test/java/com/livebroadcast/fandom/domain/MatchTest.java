package com.livebroadcast.fandom.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.livebroadcast.fandom.domain.match.Match;
import com.livebroadcast.fandom.domain.match.MatchSide;
import com.livebroadcast.fandom.domain.match.MatchStatus;

class MatchTest {

    private final Instant now = Instant.parse("2026-11-17T09:00:00Z");

    @Test
    void 같은_팀끼리는_경기를_만들_수_없다() {
        assertThatThrownBy(() -> Match.schedule(1L, 9L, 9L, now, "잠실", 1L, now)).hasMessageContaining("같을 수 없습니다");
    }

    @Test
    void 예정_진행_종료_순서로만_바뀌고_끝난_경기는_응원을_받지_않는다() {
        Match m = Match.schedule(1L, 9L, 3L, now, "잠실", 1L, now);
        m.changeStatus(MatchStatus.LIVE, now);
        m.ensureAcceptsCheers();
        m.changeStatus(MatchStatus.FINISHED, now);

        assertThatThrownBy(m::ensureAcceptsCheers).hasMessageContaining("응원할 수 없습니다");
        assertThatThrownBy(() -> m.changeStatus(MatchStatus.LIVE, now)).hasMessageContaining("바꿀 수 없습니다");
    }

    @Test
    void 경기에_나오는_팀만_홈_원정을_알려준다() {
        Match m = Match.schedule(1L, 9L, 3L, now, "잠실", 1L, now);

        assertThat(m.sideOf(9L)).isEqualTo(MatchSide.HOME);
        assertThat(m.sideOf(3L)).isEqualTo(MatchSide.AWAY);
        assertThatThrownBy(() -> m.sideOf(5L)).hasMessageContaining("경기에 나오는 팀");
    }
}
