package com.euprocuro.api.entrypoints.rest.dto.response;

import java.time.Instant;
import java.util.Map;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CacheInvalidationResponse {
    String scope;
    boolean enabled;
    String provider;
    int entries;
    Map<String, Long> versions;
    Instant invalidatedAt;
}
