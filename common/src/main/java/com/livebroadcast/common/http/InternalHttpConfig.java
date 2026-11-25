package com.livebroadcast.common.http;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestClient;

/**
 * 서비스 사이 내부 API 를 부르는 RestClient 의 빌더. 클라이언트가 RestClient.builder() 를 직접 부르지 않고 이 빈을 받으면,
 * 모니터링 SDK(ErrDeck)가 빌더에 붙어 요청을 추적하고 traceparent 헤더로 다음 서비스와 같은 추적으로 잇는다.
 * SDK 가 없으면 보통의 빌더와 같다. 빌더는 고쳐 쓰는 객체라 받을 때마다 새로 만든다(prototype).
 * 토스 같은 외부 API 는 이 빌더를 쓰지 않는다(추적 헤더를 바깥으로 보내지 않는다).
 */
@Configuration(proxyBeanMethods = false)
public class InternalHttpConfig {

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public RestClient.Builder internalRestClientBuilder() {
        return RestClient.builder();
    }
}
