package com.livebroadcast.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 외부에서 들어오는 /api/** 요청을 각 마이크로서비스로 라우팅한다.
 * 응원금 목록(/api/targets/{id}/donations)은 interaction-service, 나머지 /api/targets/** 는 fandom-service 로 간다.
 * 댓글, 후원, 실시간 이벤트는 URL 이 /api/broadcasts/{id}/... 형태라서 broadcast-service 보다 먼저 매칭되도록 order 를 낮게 준다.
 * /internal/** 는 어떤 라우트에도 걸리지 않으므로 외부에서 접근할 수 없다.
 */
@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder,
                               @Value("${services.auth-url}") String authUrl,
                               @Value("${services.broadcast-url}") String broadcastUrl,
                               @Value("${services.interaction-url}") String interactionUrl,
                               @Value("${services.fandom-url}") String fandomUrl) {
        return builder.routes()
                .route("interaction-service", r -> r.order(0)
                        .path("/api/broadcasts/*/comments/**",
                                "/api/broadcasts/*/donations/**",
                                "/api/broadcasts/*/events",
                                "/api/comments/**",
                                "/api/donations/**",
                                "/api/targets/*/donations/**",
                                "/api/viewers", "/api/viewers/**")
                        .uri(interactionUrl))
                .route("auth-service", r -> r.order(1)
                        .path("/api/auth/**")
                        .uri(authUrl))
                .route("broadcast-service", r -> r.order(2)
                        .path("/api/broadcasts", "/api/broadcasts/**",
                                "/api/recordings", "/api/recordings/**")
                        .uri(broadcastUrl))
                .route("fandom-service", r -> r.order(3)
                        .path("/api/sports/**", "/api/sports",
                                "/api/leagues/**",
                                "/api/targets", "/api/targets/**",
                                "/api/target-requests", "/api/target-requests/**",
                                "/api/fans/**",
                                "/api/hearts", "/api/hearts/**",
                                "/api/rankings", "/api/rankings/**",
                                "/api/matches", "/api/matches/**",
                                "/api/admin/**",
                                "/api/fandom/**")
                        .uri(fandomUrl))
                .build();
    }
}
