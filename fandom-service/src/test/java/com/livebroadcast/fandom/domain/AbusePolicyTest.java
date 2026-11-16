package com.livebroadcast.fandom.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.livebroadcast.fandom.domain.heart.AbusePolicy;
import com.livebroadcast.fandom.domain.heart.AbuseSignals;
import com.livebroadcast.fandom.domain.heart.HeartWeight;

class AbusePolicyTest {

    private final AbusePolicy policy = AbusePolicy.defaults();

    @Test
    void 일반_계정은_하트당_10점() {
        HeartWeight w = policy.weigh(new AbuseSignals(30, 1, 1));

        assertThat(w.pointsPerHeart()).isEqualTo(10);
        assertThat(w.suspicious()).isFalse();
    }

    @Test
    void 가입_7일_미만_계정은_절반_점수() {
        assertThat(policy.weigh(new AbuseSignals(3, 1, 1)).pointsPerHeart()).isEqualTo(5);
        assertThat(policy.weigh(new AbuseSignals(7, 1, 1)).pointsPerHeart()).isEqualTo(10);
    }

    @Test
    void 같은_기기에서_3개_이상_계정이_몰아주면_의심_하트로_0점() {
        HeartWeight w = policy.weigh(new AbuseSignals(30, 3, 3));

        assertThat(w.suspicious()).isTrue();
        assertThat(w.pointsPerHeart()).isZero();
    }

    @Test
    void IP_는_공유_와이파이를_고려해_더_느슨하게_본다() {
        assertThat(policy.weigh(new AbuseSignals(30, 1, 5)).suspicious()).isFalse();
        assertThat(policy.weigh(new AbuseSignals(30, 1, 6)).suspicious()).isTrue();
    }

    @Test
    void 가입일을_모르면_계정_나이로는_감점하지_않는다() {
        assertThat(policy.weigh(AbuseSignals.none()).pointsPerHeart()).isEqualTo(10);
    }
}
