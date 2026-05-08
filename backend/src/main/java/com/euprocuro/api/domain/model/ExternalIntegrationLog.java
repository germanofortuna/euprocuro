package com.euprocuro.api.domain.model;

import java.time.Instant;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ExternalIntegrationLog {
    private String id;
    private Instant createdAt;
    private String operation;
    private String correlationId;
    private Message request;
    private Message response;
    private Long durationMs;
    private boolean success;
    private String errorMessage;

    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String method;
        private String url;
        private Integer status;
        private Map<String, String> headers;
        private Object body;
    }
}
