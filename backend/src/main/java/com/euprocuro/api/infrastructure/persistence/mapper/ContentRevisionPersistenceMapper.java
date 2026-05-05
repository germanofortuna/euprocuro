package com.euprocuro.api.infrastructure.persistence.mapper;

import com.euprocuro.api.domain.model.ContentRevision;
import com.euprocuro.api.infrastructure.persistence.document.ContentRevisionDocument;

public final class ContentRevisionPersistenceMapper {

    private ContentRevisionPersistenceMapper() {
    }

    public static ContentRevision toDomain(ContentRevisionDocument document) {
        if (document == null) {
            return null;
        }

        return ContentRevision.builder()
                .id(document.getId())
                .contentEntryId(document.getContentEntryId())
                .key(document.getKey())
                .locale(document.getLocale())
                .version(document.getVersion())
                .snapshotValue(document.getSnapshotValue())
                .publishedBy(document.getPublishedBy())
                .publishedAt(document.getPublishedAt())
                .build();
    }

    public static ContentRevisionDocument toDocument(ContentRevision domain) {
        if (domain == null) {
            return null;
        }

        return ContentRevisionDocument.builder()
                .id(domain.getId())
                .contentEntryId(domain.getContentEntryId())
                .key(domain.getKey())
                .locale(domain.getLocale())
                .version(domain.getVersion())
                .snapshotValue(domain.getSnapshotValue())
                .publishedBy(domain.getPublishedBy())
                .publishedAt(domain.getPublishedAt())
                .build();
    }
}
