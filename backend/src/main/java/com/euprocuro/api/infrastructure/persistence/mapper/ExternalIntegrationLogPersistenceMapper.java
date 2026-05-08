package com.euprocuro.api.infrastructure.persistence.mapper;

import com.euprocuro.api.domain.model.ExternalIntegrationLog;
import com.euprocuro.api.infrastructure.persistence.document.ExternalIntegrationLogDocument;

public final class ExternalIntegrationLogPersistenceMapper {

    private ExternalIntegrationLogPersistenceMapper() {
    }

    public static ExternalIntegrationLog toDomain(ExternalIntegrationLogDocument document) {
        if (document == null) {
            return null;
        }

        return ExternalIntegrationLog.builder()
                .id(document.getId())
                .createdAt(document.getCreatedAt())
                .operation(document.getOperation())
                .correlationId(document.getCorrelationId())
                .request(toDomain(document.getRequest()))
                .response(toDomain(document.getResponse()))
                .durationMs(document.getDurationMs())
                .success(document.isSuccess())
                .errorMessage(document.getErrorMessage())
                .build();
    }

    public static ExternalIntegrationLogDocument toDocument(ExternalIntegrationLog domain) {
        if (domain == null) {
            return null;
        }

        return ExternalIntegrationLogDocument.builder()
                .id(domain.getId())
                .createdAt(domain.getCreatedAt())
                .operation(domain.getOperation())
                .correlationId(domain.getCorrelationId())
                .request(toDocument(domain.getRequest()))
                .response(toDocument(domain.getResponse()))
                .durationMs(domain.getDurationMs())
                .success(domain.isSuccess())
                .errorMessage(domain.getErrorMessage())
                .build();
    }

    private static ExternalIntegrationLog.Message toDomain(ExternalIntegrationLogDocument.Message document) {
        if (document == null) {
            return null;
        }

        return ExternalIntegrationLog.Message.builder()
                .method(document.getMethod())
                .url(document.getUrl())
                .status(document.getStatus())
                .headers(document.getHeaders())
                .body(document.getBody())
                .build();
    }

    private static ExternalIntegrationLogDocument.Message toDocument(ExternalIntegrationLog.Message domain) {
        if (domain == null) {
            return null;
        }

        return ExternalIntegrationLogDocument.Message.builder()
                .method(domain.getMethod())
                .url(domain.getUrl())
                .status(domain.getStatus())
                .headers(domain.getHeaders())
                .body(domain.getBody())
                .build();
    }
}
