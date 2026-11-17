-- 편파방: 방송과 녹화에 응원팀(fandom-service 의 응원 대상)과 경기를 기록한다.
-- 모두 NULL 허용이라 기존 방송, 녹화와 과제 필수 흐름(응원팀 없이 방송 만들기)은 그대로 동작한다.
ALTER TABLE broadcasts
    ADD COLUMN league_code       VARCHAR(20),
    ADD COLUMN target_id         BIGINT,
    ADD COLUMN target_name       VARCHAR(40),
    ADD COLUMN target_short_name VARCHAR(6),
    ADD COLUMN target_color      VARCHAR(7),
    ADD COLUMN match_id          BIGINT;

CREATE INDEX idx_broadcasts_target_status ON broadcasts (target_id, status);
CREATE INDEX idx_broadcasts_league_status ON broadcasts (league_code, status);
CREATE INDEX idx_broadcasts_match ON broadcasts (match_id);

ALTER TABLE recordings
    ADD COLUMN league_code       VARCHAR(20),
    ADD COLUMN target_id         BIGINT,
    ADD COLUMN target_name       VARCHAR(40),
    ADD COLUMN target_short_name VARCHAR(6),
    ADD COLUMN target_color      VARCHAR(7),
    ADD COLUMN match_id          BIGINT;

CREATE INDEX idx_recordings_target_status ON recordings (target_id, status, started_at DESC);
