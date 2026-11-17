package com.livebroadcast.broadcast.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.livebroadcast.broadcast.app.port.out.FandomQueryPort;
import com.livebroadcast.broadcast.domain.FanSide;

/** 편파방 응원팀 지정 규칙과 팬덤 서비스 장애 시 동작 */
class FanSideResolverTest {

    static final long DOOSAN = 1L;
    static final long LG = 2L;

    boolean down;

    final FandomQueryPort fandom = new FandomQueryPort() {
        @Override
        public Optional<TargetInfo> findTarget(Long targetId) {
            if (down) throw new FandomUnavailableException("timeout");
            if (targetId == DOOSAN) return Optional.of(new TargetInfo(DOOSAN, "KBO", "두산 베어스", "두산", "#131230", "#FFFFFF", "ACTIVE"));
            if (targetId == 99L) return Optional.of(new TargetInfo(99L, "KBO", "숨김팀", "숨김", "#000000", "HIDDEN"));
            return Optional.empty();
        }

        @Override
        public Optional<MatchInfo> findMatch(Long matchId) {
            if (matchId == 10L) return Optional.of(new MatchInfo(10L, "KBO", DOOSAN, LG, "LIVE"));
            if (matchId == 11L) return Optional.of(new MatchInfo(11L, "KBO", DOOSAN, LG, "FINISHED"));
            return Optional.empty();
        }
    };

    final FanSideResolver resolver = new FanSideResolver(fandom);

    @Test
    void 응원팀이_없으면_일반_방송이다() {
        assertThat(resolver.resolve(null, null)).isEqualTo(FanSide.NONE);
    }

    @Test
    void 응원팀_이름과_색을_방송에_복사해_둔다() {
        FanSide side = resolver.resolve(DOOSAN, 10L);

        assertThat(side.targetShortName()).isEqualTo("두산");
        assertThat(side.leagueCode()).isEqualTo("KBO");
        assertThat(side.targetColor()).isEqualTo("#131230");
        assertThat(side.targetTextColor()).isEqualTo("#FFFFFF");
        assertThat(side.matchId()).isEqualTo(10L);
    }

    @Test
    void 없는_대상이나_숨김_대상은_거절한다() {
        assertThatThrownBy(() -> resolver.resolve(404L, null)).hasMessageContaining("응원팀을 찾을 수 없거나");
        assertThatThrownBy(() -> resolver.resolve(99L, null)).hasMessageContaining("응원팀을 찾을 수 없거나");
    }

    @Test
    void 끝난_경기나_응원팀이_안_나오는_경기는_지정할_수_없다() {
        assertThatThrownBy(() -> resolver.resolve(DOOSAN, 11L)).hasMessageContaining("경기만");
        assertThatThrownBy(() -> resolver.resolve(null, 10L)).hasMessageContaining("응원팀을 먼저");
    }

    @Test
    void 팬덤_서비스가_죽어도_방송은_일반_방송으로_만들어진다() {
        down = true;

        assertThat(resolver.resolve(DOOSAN, 10L)).isEqualTo(FanSide.NONE);
    }
}
