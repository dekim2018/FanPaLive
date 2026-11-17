package com.livebroadcast.broadcast.out.persistence;

import com.livebroadcast.broadcast.domain.FanSide;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** 방송과 녹화가 함께 쓰는 편파방 컬럼(모두 NULL 허용) */
@Embeddable
public class FanSideColumns {

    @Column(name = "league_code", length = 20)
    private String leagueCode;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "target_name", length = 40)
    private String targetName;

    @Column(name = "target_short_name", length = 6)
    private String targetShortName;

    @Column(name = "target_color", length = 7)
    private String targetColor;

    @Column(name = "target_text_color", length = 7)
    private String targetTextColor;

    @Column(name = "match_id")
    private Long matchId;

    protected FanSideColumns() {
    }

    static FanSideColumns from(FanSide side) {
        FanSideColumns c = new FanSideColumns();
        if (side != null && side.isPresent()) {
            c.leagueCode = side.leagueCode();
            c.targetId = side.targetId();
            c.targetName = side.targetName();
            c.targetShortName = side.targetShortName();
            c.targetColor = side.targetColor();
            c.targetTextColor = side.targetTextColor();
            c.matchId = side.matchId();
        }
        return c;
    }

    /** 컬럼이 모두 비어 있으면 Hibernate 가 null 을 넘겨준다. */
    static FanSide toDomain(FanSideColumns c) {
        if (c == null || c.targetId == null) {
            return FanSide.NONE;
        }
        return new FanSide(c.leagueCode, c.targetId, c.targetName, c.targetShortName, c.targetColor,
                c.targetTextColor, c.matchId);
    }
}
