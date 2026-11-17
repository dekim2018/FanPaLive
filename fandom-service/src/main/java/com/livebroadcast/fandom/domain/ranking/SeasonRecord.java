package com.livebroadcast.fandom.domain.ranking;

import java.util.List;

/**
 * 명예의 전당 한 시즌: 최종 순위와 우승팀 최고 서포터(MVP 팬).
 *
 * @param mvp 우승팀에 가장 많이 기여한 팬. 기록이 없으면 null
 */
public record SeasonRecord(String seasonKey, List<SeasonRow> rows, Supporter mvp) {

    public record SeasonRow(int rank, Long targetId, String name, String shortName, String colorPrimary,
                            String colorSecondary, long score) {
    }
}
