package com.livebroadcast.interaction.out.client;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.livebroadcast.common.error.DomainException;
import com.livebroadcast.common.error.ErrorType;
import com.livebroadcast.common.security.InternalApiKeyFilter;
import com.livebroadcast.interaction.app.port.out.FanTargetQueryPort;

/** fandom-service 내부 API 로 응원 대상(구단/선수)이 실제로 있고 공개 중인지 확인한다. */
@Component
public class FandomServiceClient implements FanTargetQueryPort {

    private final RestClient restClient;

    public FandomServiceClient(@Value("${services.fandom-url}") String baseUrl,
                               @Value("${app.internal.api-key}") String internalApiKey,
            RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(1));
        factory.setReadTimeout(Duration.ofSeconds(2));
        // 공통 빌더(InternalHttpConfig)를 받아 요청 추적이 다음 서비스로 이어지게 한다.
        this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(InternalApiKeyFilter.HEADER, internalApiKey)
                .build();
    }

    @Override
    public Optional<FanTargetInfo> findTarget(Long targetId) {
        try {
            return restClient.get()
                    .uri("/internal/targets/{id}", targetId)
                    .exchange((request, response) -> {
                        if (response.getStatusCode().value() == 404) {
                            return Optional.<FanTargetInfo>empty();
                        }
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            throw unavailable();
                        }
                        TargetDto dto = response.bodyTo(TargetDto.class);
                        return dto == null ? Optional.<FanTargetInfo>empty()
                                : Optional.of(new FanTargetInfo(dto.id(), dto.name(), dto.status()));
                    });
        } catch (RestClientException e) {
            throw unavailable();
        }
    }

    private static DomainException unavailable() {
        return new DomainException(ErrorType.BAD_GATEWAY, "FANDOM_SERVICE_UNAVAILABLE",
                "응원 대상 정보를 확인할 수 없습니다. 잠시 후 다시 시도해 주세요.");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TargetDto(Long id, String name, String status) {
    }
}
