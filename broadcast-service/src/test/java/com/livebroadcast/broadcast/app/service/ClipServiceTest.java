package com.livebroadcast.broadcast.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.livebroadcast.broadcast.app.port.in.ClipUseCase.CreateClip;
import com.livebroadcast.broadcast.domain.Clip;
import com.livebroadcast.broadcast.domain.FanSide;
import com.livebroadcast.broadcast.domain.Recording;
import com.livebroadcast.broadcast.domain.RecordingStatus;
import com.livebroadcast.broadcast.support.FakeClipRepository;
import com.livebroadcast.broadcast.support.FakeRecordingRepository;

class ClipServiceTest {

    static final Instant T0 = Instant.parse("2026-11-10T10:00:00Z");
    static final Long OWNER = 7L;

    final FakeRecordingRepository recordings = new FakeRecordingRepository();
    final FakeClipRepository clips = new FakeClipRepository();
    final ClipService service = new ClipService(clips, recordings, Clock.fixed(T0.plusSeconds(9000), ZoneOffset.UTC));

    Recording recording(RecordingStatus status) {
        return recordings.save(new Recording(null, 1L, OWNER, "방송자", "두산 편파중계", "live/1", "/recordings/live/1",
                status, T0, status == RecordingStatus.READY ? T0.plusSeconds(600) : null, 600, 3, null, T0,
                new FanSide("KBO", 9L, "두산 베어스", "두산", "#131230", "#FFFFFF", null)));
    }

    @Test
    void 녹화의_구간만_기록하고_응원팀_정보를_물려받는다() {
        Recording r = recording(RecordingStatus.READY);

        Clip clip = service.create(new CreateClip(r.getId(), 3L, "곰돌", "", 120, 30));

        assertThat(clip.getStartAt()).isEqualTo(T0.plusSeconds(120));
        assertThat(clip.getTitle()).isEqualTo("두산 편파중계 하이라이트");
        assertThat(clip.getSide().targetId()).isEqualTo(9L);
        assertThat(service.list(9L, null, 0, 20).content()).hasSize(1);
    }

    @Test
    void 녹화_중이거나_길이를_벗어나면_만들_수_없다() {
        Recording live = recording(RecordingStatus.RECORDING);
        Recording ready = recording(RecordingStatus.READY);

        assertThatThrownBy(() -> service.create(new CreateClip(live.getId(), 3L, "곰돌", null, 0, 30)))
                .hasMessageContaining("녹화가 끝난");
        assertThatThrownBy(() -> service.create(new CreateClip(ready.getId(), 3L, "곰돌", null, 590, 30)))
                .hasMessageContaining("벗어났습니다");
        assertThatThrownBy(() -> service.create(new CreateClip(ready.getId(), 3L, "곰돌", null, 0, 61)))
                .hasMessageContaining("60초");
    }

    @Test
    void 만든_사람과_녹화_소유자와_관리자만_지울_수_있다() {
        Recording r = recording(RecordingStatus.READY);
        Clip a = service.create(new CreateClip(r.getId(), 3L, "곰돌", null, 0, 10));
        Clip b = service.create(new CreateClip(r.getId(), 3L, "곰돌", null, 10, 10));
        Clip c = service.create(new CreateClip(r.getId(), 3L, "곰돌", null, 20, 10));

        assertThatThrownBy(() -> service.delete(a.getId(), 99L, false)).hasMessageContaining("삭제할 수 있습니다");
        service.delete(a.getId(), 3L, false);
        service.delete(b.getId(), OWNER, false);
        service.delete(c.getId(), 99L, true);

        assertThat(clips.byId).isEmpty();
    }
}
