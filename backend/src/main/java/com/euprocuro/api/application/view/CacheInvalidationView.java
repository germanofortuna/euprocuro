package com.euprocuro.api.application.view;

import java.time.Instant;
import java.util.Map;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CacheInvalidationView {
    String scope;
    boolean enabled;
    String provider;
    int entries;
    Map<String, Long> versions;
    Instant invalidatedAt;
}
