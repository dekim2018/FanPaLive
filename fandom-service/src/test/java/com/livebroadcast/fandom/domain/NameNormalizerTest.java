package com.livebroadcast.fandom.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.livebroadcast.fandom.domain.target.NameNormalizer;

class NameNormalizerTest {

    @Test
    void 공백_대소문자_기호_차이는_같은_이름으로_본다() {
        assertThat(NameNormalizer.normalize("Tottenham Hotspur"))
                .isEqualTo(NameNormalizer.normalize(" tottenham-hotspur "));
        assertThat(NameNormalizer.normalize("토트넘 홋스퍼")).isEqualTo(NameNormalizer.normalize("토트넘홋스퍼"));
    }

    @Test
    void 전각_문자도_반각으로_맞춘다() {
        assertThat(NameNormalizer.normalize("ＮＢＡ")).isEqualTo("nba");
    }
}
