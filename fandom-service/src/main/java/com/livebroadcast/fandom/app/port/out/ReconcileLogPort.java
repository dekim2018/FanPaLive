package com.livebroadcast.fandom.app.port.out;

import java.time.Instant;
import java.time.LocalDate;

public interface ReconcileLogPort {

    void record(LocalDate statDate, Long targetId, long expected, long actual, Instant at);
}
