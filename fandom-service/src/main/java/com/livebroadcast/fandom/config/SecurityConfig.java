package com.livebroadcast.fandom.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.livebroadcast.common.security.ApiSecuritySupport;

/**
 * 종목, 대상, 랭킹, 경기 조회는 비로그인 허용. 팬 등록, 하트, 등록 신청은 로그인 필수.
 * 관리자 API(/api/admin/**)는 ROLE_ADMIN 만 허용하고, 컨트롤러에서도 한 번 더 확인한다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ApiSecuritySupport support) throws Exception {
        support.applyDefaults(http)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/internal/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/fans/me", "/api/hearts/me",
                                "/api/target-requests/mine").authenticated()
                        .requestMatchers(HttpMethod.GET,
                                "/api/fandom/config",
                                "/api/sports", "/api/leagues/**",
                                "/api/targets", "/api/targets/**",
                                "/api/target-requests",
                                "/api/rankings", "/api/rankings/**",
                                "/api/matches", "/api/matches/**",
                                "/api/fans/badges").permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
