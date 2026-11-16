package com.livebroadcast.fandom.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.livebroadcast.fandom.domain.target.TargetProfile;

class TargetProfileTest {

    @Test
    void 약칭과_색을_비우면_기본값을_쓴다() {
        TargetProfile p = new TargetProfile("두산 베어스", null, null, null, null, null);

        assertThat(p.shortName()).isEqualTo("두산");
        assertThat(p.colorPrimary()).startsWith("#");
    }

    @Test
    void 색은_RRGGBB_형식만_받는다() {
        assertThatThrownBy(() -> new TargetProfile("두산 베어스", null, "두산", "navy", null, null))
                .hasMessageContaining("#RRGGBB");
        assertThat(new TargetProfile("두산 베어스", null, "두산", "#131230", null, null).colorPrimary())
                .isEqualTo("#131230");
    }

    @Test
    void 이름은_2자_이상이어야_한다() {
        assertThatThrownBy(() -> new TargetProfile("a", null, null, null, null, null)).hasMessageContaining("2~40");
    }
}
