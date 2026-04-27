package com.euprocuro.api.infrastructure.realtime;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RealtimeEventEnvelope {
    String type;
    Instant createdAt;
    Object payload;
}
