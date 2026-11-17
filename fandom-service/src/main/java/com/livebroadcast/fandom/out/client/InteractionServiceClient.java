package com.livebroadcast.fandom.out.client;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.livebroadcast.common.security.InternalApiKeyFilter;
import com.livebroadcast.fandom.app.port.out.DonationTotalsPort;

/** 결제 원본(interaction-service)의 하루 후원 합계를 내부 API 로 조회한다(대사용). */
@Component
public class InteractionServiceClient implements DonationTotalsPort {

    private final RestClient restClient;

    public InteractionServiceClient(@Value("${services.interaction-url}") String baseUrl,
                                    @Value("${app.internal.api-key}") String internalApiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(InternalApiKeyFilter.HEADER, internalApiKey)
                .build();
    }

    @Override
    public Map<Long, Long> findTargetTotals(LocalDate date) {
        List<TargetTotal> totals = restClient.get()
                .uri("/internal/donations/target-totals?date={date}", date)
                .retrieve()
                .body(new ParameterizedTypeReference<List<TargetTotal>>() {
                });
        Map<Long, Long> result = new HashMap<>();
        if (totals != null) {
            totals.forEach(t -> result.put(t.targetId(), t.amount()));
        }
        return result;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TargetTotal(Long targetId, long amount) {
    }
}
