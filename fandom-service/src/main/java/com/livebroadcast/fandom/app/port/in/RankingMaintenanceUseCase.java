package com.livebroadcast.fandom.app.port.in;

import java.time.LocalDate;

public interface RankingMaintenanceUseCase {

    /** DB(원본) 기준으로 모든 리그의 실시간 순위표를 다시 만든다. */
    void rebuildAll();

    /** 결제 원본과 후원 집계를 비교해서 다르면 원본 기준으로 맞춘다. 맞춘 대상 수를 돌려준다. */
    int reconcileDonations(LocalDate date);

    /** 지난주 최종 순위를 저장한다(순위 변동, 순위 추이 표시용). */
    void snapshotPreviousWeek();
}
