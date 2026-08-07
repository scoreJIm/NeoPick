package com.neopick.port.messaging;

import com.neopick.domain.common.DomainEvent;

public interface EventPublisher {

    void publish(String topic, DomainEvent event);
}
