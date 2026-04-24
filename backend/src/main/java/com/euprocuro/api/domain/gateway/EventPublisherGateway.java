package com.euprocuro.api.domain.gateway;

import java.util.Map;

public interface EventPublisherGateway {
    void publish(String eventType, Map<String, Object> payload);
}
