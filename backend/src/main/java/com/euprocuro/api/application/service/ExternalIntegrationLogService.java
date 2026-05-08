package com.euprocuro.api.application.service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.euprocuro.api.domain.gateway.ExternalIntegrationLogGateway;
import com.euprocuro.api.domain.model.ExternalIntegrationLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalIntegrationLogService {

    private static final String MASKED = "***";

    private final ExternalIntegrationLogGateway externalIntegrationLogGateway;
    private final ObjectMapper objectMapper;

    @Value("${application.audit.external-log-body-max-length:4000}")
    private int bodyMaxLength = 4000;

    public Instant startedAt() {
        return Instant.now();
    }

    public void recordSuccess(
            String operation,
            String correlationId,
            String method,
            String url,
            Map<String, String> requestHeaders,
            Object requestBody,
            Integer responseStatus,
            Object responseBody,
            Instant startedAt
    ) {
        record(operation, correlationId, method, url, requestHeaders, requestBody,
                responseStatus, responseBody, startedAt, true, null);
    }

    public void recordFailure(
            String operation,
            String correlationId,
            String method,
            String url,
            Map<String, String> requestHeaders,
            Object requestBody,
            Integer responseStatus,
            Object responseBody,
            Instant startedAt,
            Exception exception
    ) {
        record(operation, correlationId, method, url, requestHeaders, requestBody,
                responseStatus, responseBody, startedAt, false, exception == null ? null : exception.getMessage());
    }

    private void record(
            String operation,
            String correlationId,
            String method,
            String url,
            Map<String, String> requestHeaders,
            Object requestBody,
            Integer responseStatus,
            Object responseBody,
            Instant startedAt,
            boolean success,
            String errorMessage
    ) {
        try {
            Instant now = Instant.now();
            externalIntegrationLogGateway.save(ExternalIntegrationLog.builder()
                    .createdAt(now)
                    .operation(operation)
                    .correlationId(correlationId)
                    .request(ExternalIntegrationLog.Message.builder()
                            .method(method)
                            .url(url)
                            .headers(sanitizeHeaders(requestHeaders))
                            .body(toSafeBody(requestBody))
                            .build())
                    .response(ExternalIntegrationLog.Message.builder()
                            .status(responseStatus)
                            .body(toSafeBody(responseBody))
                            .build())
                    .durationMs(startedAt == null ? null : Duration.between(startedAt, now).toMillis())
                    .success(success)
                    .errorMessage(truncate(errorMessage))
                    .build());
        } catch (RuntimeException exception) {
            log.warn("Nao foi possivel gravar log externo '{}' no Mongo. Aplicacao seguira normalmente. {}",
                    operation, exception.getMessage());
        }
    }

    private Map<String, String> sanitizeHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }

        Map<String, String> sanitized = new LinkedHashMap<>();
        headers.forEach((key, value) -> sanitized.put(key, isSensitive(key) ? MASKED : truncate(value)));
        return sanitized;
    }

    private boolean isSensitive(String key) {
        String normalized = Optional.ofNullable(key).orElse("").toLowerCase(Locale.ROOT);
        return normalized.contains("authorization")
                || normalized.contains("token")
                || normalized.contains("password")
                || normalized.contains("secret")
                || normalized.contains("cookie")
                || normalized.contains("key");
    }

    private Object toSafeBody(Object body) {
        if (body == null) {
            return null;
        }
        if (body instanceof String) {
            return truncate((String) body);
        }
        try {
            String json = objectMapper.writeValueAsString(body);
            if (json.length() > safeMaxLength()) {
                return truncate(json);
            }
            return objectMapper.convertValue(body, Object.class);
        } catch (JsonProcessingException exception) {
            return truncate(String.valueOf(body));
        } catch (IllegalArgumentException exception) {
            return truncate(String.valueOf(body));
        }
    }

    private String truncate(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        int safeMaxLength = safeMaxLength();
        return value.length() <= safeMaxLength ? value : value.substring(0, safeMaxLength) + "...[truncated]";
    }

    private int safeMaxLength() {
        return Math.max(250, bodyMaxLength);
    }
}
