package com.euprocuro.api.shared.config;

import java.time.Duration;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuditMongoIndexConfig {

    private static final String AUDIT_EVENTS = "audit_events";
    private static final String EXTERNAL_INTEGRATION_LOGS = "external_integration_logs";

    private final MongoTemplate mongoTemplate;

    @Value("${application.audit.ttl-seconds:604800}")
    private long auditTtlSeconds;

    @Value("${application.audit.external-log-ttl-seconds:604800}")
    private long externalLogTtlSeconds;

    @PostConstruct
    public void ensureIndexes() {
        ensureTtlIndex(AUDIT_EVENTS, "audit_events_ttl_idx", auditTtlSeconds);
        ensureTtlIndex(EXTERNAL_INTEGRATION_LOGS, "external_integration_logs_ttl_idx", "createdAt", externalLogTtlSeconds);
    }

    private void ensureTtlIndex(String collection, String name, long ttlSeconds) {
        ensureTtlIndex(collection, name, "occurredAt", ttlSeconds);
    }

    private void ensureTtlIndex(String collection, String name, String field, long ttlSeconds) {
        IndexOperations indexOperations = mongoTemplate.indexOps(collection);
        indexOperations.getIndexInfo()
                .stream()
                .filter(index -> name.equals(index.getName()))
                .filter(index -> !hasField(index, field))
                .findFirst()
                .ifPresent(index -> indexOperations.dropIndex(name));

        indexOperations.ensureIndex(new Index()
                .on(field, Direction.ASC)
                .expire(Duration.ofSeconds(Math.max(60, ttlSeconds)))
                .named(name));
    }

    private boolean hasField(IndexInfo index, String field) {
        return index.getIndexFields()
                .stream()
                .anyMatch(indexField -> field.equals(indexField.getKey()));
    }
}
