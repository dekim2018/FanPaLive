package com.livebroadcast.fandom.app.port.out;

public interface DomainEventPublisherPort {

    void publish(Object event);
}
