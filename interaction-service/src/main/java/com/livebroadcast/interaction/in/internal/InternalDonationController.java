package com.livebroadcast.interaction.in.internal;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.livebroadcast.interaction.app.port.in.DonationUseCase;

/**
 * fandom-service 의 새벽 대사가 호출한다. 결제 원본 기준 "하루 동안 응원 대상별로 확정된 후원 합계".
 * 스트림 이벤트를 놓쳐 랭킹 점수가 빠진 경우 이 값으로 맞춘다.
 */
@RestController
public class InternalDonationController {

    private final DonationUseCase donations;

    public InternalDonationController(DonationUseCase donations) {
        this.donations = donations;
    }

    @GetMapping("/internal/donations/target-totals")
    public List<TargetTotal> targetTotals(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return donations.targetTotals(date).entrySet().stream()
                .map(e -> new TargetTotal(e.getKey(), e.getValue()))
                .toList();
    }

    public record TargetTotal(Long targetId, long amount) {
    }
}
