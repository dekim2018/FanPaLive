package com.livebroadcast.fandom.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.livebroadcast.fandom.domain.fan.PrimaryFanPolicy;

class PrimaryFanPolicyTest {

    private final PrimaryFanPolicy policy = new PrimaryFanPolicy(Duration.ofDays(30));
    private final Instant changed = Instant.parse("2026-11-01T00:00:00Z");

    @Test
    void 처음_정할_때는_제한이_없다() {
        policy.ensureCanChange(null, changed);
        assertThat(policy.remainingDays(null, changed)).isZero();
    }

    @Test
    void 바꾼_뒤_30일_동안은_다시_바꿀_수_없고_남은_일수를_알려준다() {
        Instant tenDaysLater = changed.plus(Duration.ofDays(10));

        assertThat(policy.remainingDays(changed, tenDaysLater)).isEqualTo(20L);
        assertThatThrownBy(() -> policy.ensureCanChange(changed, tenDaysLater)).hasMessageContaining("20일");
    }

    @Test
    void 정확히_30일이_지나면_바꿀_수_있다() {
        policy.ensureCanChange(changed, changed.plus(Duration.ofDays(30)));
        assertThat(policy.remainingDays(changed, changed.plus(Duration.ofDays(30)).minusSeconds(1))).isEqualTo(1L);
    }
}
