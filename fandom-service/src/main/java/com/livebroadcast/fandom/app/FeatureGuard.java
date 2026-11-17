package com.livebroadcast.fandom.app;

import org.springframework.stereotype.Component;

import com.livebroadcast.common.error.DomainException;

@Component
public class FeatureGuard {

    private final FandomProperties properties;

    public FeatureGuard(FandomProperties properties) {
        this.properties = properties;
    }

    public void hearts() {
        if (!properties.features().hearts()) throw disabled();
    }

    public void matches() {
        if (!properties.features().matches()) throw disabled();
    }

    public void targetRequests() {
        if (!properties.features().targetRequests()) throw disabled();
    }

    private static DomainException disabled() {
        return DomainException.notFound("FEATURE_DISABLED", "지금은 사용할 수 없는 기능입니다.");
    }
}
