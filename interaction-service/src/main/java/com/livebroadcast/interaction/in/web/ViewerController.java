package com.livebroadcast.interaction.in.web;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.livebroadcast.interaction.app.service.ViewerService;
import com.livebroadcast.interaction.app.service.ViewerService.ViewerStats;

/** 방송 카드와 시청 화면에 보여 줄 시청자 수 */
@RestController
public class ViewerController {

    private final ViewerService viewers;

    public ViewerController(ViewerService viewers) {
        this.viewers = viewers;
    }

    /** 목록 화면이 방송 여러 개의 시청자 수를 한 번에 가져간다. 예: /api/viewers?ids=1,2,3 */
    @GetMapping("/api/viewers")
    public Map<Long, ViewerStats> stats(@RequestParam String ids) {
        List<Long> parsed = Arrays.stream(ids.split(","))
                .map(String::strip)
                .filter(s -> s.matches("\\d{1,18}"))
                .map(Long::valueOf)
                .toList();
        return viewers.stats(parsed);
    }

    @GetMapping("/api/viewers/targets/{targetId}")
    public Map<String, Object> target(@PathVariable Long targetId) {
        return Map.of("targetId", targetId, "monthlyUniqueViewers", viewers.targetMonthlyUniqueViewers(targetId));
    }
}
