package com.euprocuro.api.infrastructure.persistence.mapper;

import com.euprocuro.api.domain.model.AuditEvent;
import com.euprocuro.api.infrastructure.persistence.document.AuditEventDocument;

public final class AuditEventPersistenceMapper {

    private AuditEventPersistenceMapper() {
    }

    public static AuditEvent toDomain(AuditEventDocument document) {
        if (document == null) {
            return null;
        }

        return AuditEvent.builder()
                .id(document.getId())
                .occurredAt(document.getOccurredAt())
                .action(document.getAction())
                .actorUserId(document.getActorUserId())
                .actorEmail(document.getActorEmail())
                .resourceType(document.getResourceType())
                .resourceId(document.getResourceId())
                .outcome(document.getOutcome())
                .metadata(document.getMetadata())
                .build();
    }

    public static AuditEventDocument toDocument(AuditEvent domain) {
        if (domain == null) {
            return null;
        }

        return AuditEventDocument.builder()
                .id(domain.getId())
                .occurredAt(domain.getOccurredAt())
                .action(domain.getAction())
                .actorUserId(domain.getActorUserId())
                .actorEmail(domain.getActorEmail())
                .resourceType(domain.getResourceType())
                .resourceId(domain.getResourceId())
                .outcome(domain.getOutcome())
                .metadata(domain.getMetadata())
                .build();
    }
}
