package com.livebroadcast.interaction.in.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.livebroadcast.interaction.app.service.ViewerService;
import com.livebroadcast.interaction.out.event.SseEventHub;

/**
 * 시청 화면이 구독하는 실시간 이벤트 스트림(댓글, 후원). 비로그인 시청자도 구독할 수 있다.
 */
@RestController
public class LiveEventController {

    private final SseEventHub eventHub;
    private final ViewerService viewers;

    public LiveEventController(SseEventHub eventHub, ViewerService viewers) {
        this.eventHub = eventHub;
        this.viewers = viewers;
    }

    /**
     * @param vid 브라우저가 만들어 저장해 둔 시청자 식별값. EventSource 는 헤더를 못 보내서 쿼리로 받는다.
     *            누적 순시청자(HyperLogLog) 집계에만 쓰고, 재연결로 같은 값이 다시 와도 한 명으로 센다.
     */
    @GetMapping(value = "/api/broadcasts/{broadcastId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable Long broadcastId, @RequestParam(required = false) String vid) {
        viewers.recordView(broadcastId, null, vid);
        return eventHub.subscribe(broadcastId);
    }
}
