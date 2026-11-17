package com.livebroadcast.fandom.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.livebroadcast.fandom.domain.target.RequestStatus;
import com.livebroadcast.fandom.domain.target.TargetKind;
import com.livebroadcast.fandom.domain.target.TargetProfile;
import com.livebroadcast.fandom.domain.target.TargetRequest;

class TargetRequestTest {

    private final Instant now = Instant.parse("2026-11-17T00:00:00Z");

    private TargetRequest open() {
        return TargetRequest.open(2L, TargetKind.TEAM, new TargetProfile("토트넘 홋스퍼", "Tottenham Hotspur", "토트넘",
                "#132257", null, ""), "https://example.com", 7L, "신청자", now);
    }

    @Test
    void 신청하면_신청자_표_1개로_시작하고_투표로_늘어난다() {
        TargetRequest r = open();
        r.addVote();

        assertThat(r.getVoteCount()).isEqualTo(2);
        assertThat(r.getStatus()).isEqualTo(RequestStatus.PENDING);
    }

    @Test
    void 반려는_사유가_있어야_하고_처리된_신청은_다시_처리할_수_없다() {
        TargetRequest r = open();
        assertThatThrownBy(() -> r.reject(1L, " ", now)).hasMessageContaining("사유");

        r.approve(1L, 99L, now);

        assertThat(r.getTargetId()).isEqualTo(99L);
        assertThatThrownBy(() -> r.reject(1L, "중복", now)).hasMessageContaining("이미 처리");
        assertThatThrownBy(r::addVote).hasMessageContaining("이미 처리");
    }

    @Test
    void 잘못된_참고_링크는_저장하지_않는다() {
        TargetRequest r = TargetRequest.open(2L, TargetKind.TEAM, new TargetProfile("아스널", null, null, null, null,
                null), "javascript:alert(1)", 7L, "신청자", now);

        assertThat(r.getReferenceUrl()).isNull();
    }
}
