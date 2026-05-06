package com.euprocuro.api.infrastructure.persistence.document;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document("external_integration_logs")
@CompoundIndexes({
        @CompoundIndex(name = "external_operation_created_idx", def = "{'operation': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "external_correlation_created_idx", def = "{'correlationId': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "external_success_created_idx", def = "{'success': 1, 'createdAt': -1}")
})
public class ExternalIntegrationLogDocument {
    @Id
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
