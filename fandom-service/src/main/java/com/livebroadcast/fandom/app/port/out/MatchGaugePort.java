package com.livebroadcast.fandom.app.port.out;

import com.livebroadcast.fandom.domain.match.MatchGauge;
import com.livebroadcast.fandom.domain.match.MatchSide;

public interface MatchGaugePort {

    void add(Long matchId, MatchSide side, long hearts);

    MatchGauge read(Long matchId);
}
