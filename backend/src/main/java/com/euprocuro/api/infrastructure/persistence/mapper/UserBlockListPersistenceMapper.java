package com.euprocuro.api.infrastructure.persistence.mapper;

import com.euprocuro.api.domain.model.UserBlockListEntry;
import com.euprocuro.api.infrastructure.persistence.document.UserBlockListEntryDocument;

public final class UserBlockListPersistenceMapper {

    private UserBlockListPersistenceMapper() {
    }

    public static UserBlockListEntry toDomain(UserBlockListEntryDocument document) {
        if (document == null) {
            return null;
        }

        return UserBlockListEntry.builder()
                .id(document.getId())
                .userId(document.getUserId())
                .userEmail(document.getUserEmail())
                .documentHash(document.getDocumentHash())
                .documentLast4(document.getDocumentLast4())
                .documentType(document.getDocumentType())
                .active(document.isActive())
                .sourceProvider(document.getSourceProvider())
                .sourceInterestId(document.getSourceInterestId())
                .reason(document.getReason())
                .occurrenceCount(document.getOccurrenceCount())
                .firstBlockedAt(document.getFirstBlockedAt())
                .lastBlockedAt(document.getLastBlockedAt())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    public static UserBlockListEntryDocument toDocument(UserBlockListEntry domain) {
        if (domain == null) {
            return null;
        }

        return UserBlockListEntryDocument.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .userEmail(domain.getUserEmail())
                .documentHash(domain.getDocumentHash())
                .documentLast4(domain.getDocumentLast4())
                .documentType(domain.getDocumentType())
                .active(domain.isActive())
                .sourceProvider(domain.getSourceProvider())
                .sourceInterestId(domain.getSourceInterestId())
                .reason(domain.getReason())
                .occurrenceCount(domain.getOccurrenceCount())
                .firstBlockedAt(domain.getFirstBlockedAt())
                .lastBlockedAt(domain.getLastBlockedAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
