package com.livebroadcast.broadcast.out.client;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.livebroadcast.broadcast.app.port.out.FandomQueryPort;
import com.livebroadcast.common.security.InternalApiKeyFilter;

/**
 * fandom-service 내부 API 로 응원 대상과 경기를 확인한다.
 * 타임아웃을 짧게(연결 1초, 응답 2초) 둬서 팬덤 서비스가 느려도 방송 만들기가 오래 멈추지 않게 한다.
 */
@Component
public class FandomServiceClient implements FandomQueryPort {

    private final RestClient restClient;

    public FandomServiceClient(@Value("${services.fandom-url}") String baseUrl,
                               @Value("${app.internal.api-key}") String internalApiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(1));
        factory.setReadTimeout(Duration.ofSeconds(2));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(InternalApiKeyFilter.HEADER, internalApiKey)
                .build();
    }

    @Override
    public Optional<TargetInfo> findTarget(Long targetId) {
        return get("/internal/targets/{id}", targetId, TargetDto.class)
                .map(d -> new TargetInfo(d.id(), d.leagueCode(), d.name(), d.shortName(), d.colorPrimary(),
                        d.colorSecondary(), d.status()));
    }

    @Override
    public Optional<MatchInfo> findMatch(Long matchId) {
        return get("/internal/matches/{id}", matchId, MatchDto.class)
                .map(d -> new MatchInfo(d.id(), d.leagueCode(), d.homeTargetId(), d.awayTargetId(), d.status()));
    }

    private <T> Optional<T> get(String uri, Long id, Class<T> type) {
        try {
            return restClient.get().uri(uri, id).exchange((request, response) -> {
                if (response.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                    return Optional.<T>empty();
                }
                if (!response.getStatusCode().is2xxSuccessful()) {
                    throw new FandomUnavailableException("status " + response.getStatusCode().value());
                }
                return Optional.ofNullable(response.bodyTo(type));
            });
        } catch (RestClientException e) {
            throw new FandomUnavailableException(e.getMessage());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TargetDto(Long id, String leagueCode, String name, String shortName, String colorPrimary,
                     String colorSecondary, String status) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MatchDto(Long id, String leagueCode, Long homeTargetId, Long awayTargetId, String status) {
    }
}
