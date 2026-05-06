package com.euprocuro.api.infrastructure.persistence.mapper;

import com.euprocuro.api.domain.model.ContentEntry;
import com.euprocuro.api.infrastructure.persistence.document.ContentEntryDocument;

public final class ContentEntryPersistenceMapper {

    private ContentEntryPersistenceMapper() {
    }

    public static ContentEntry toDomain(ContentEntryDocument document) {
        if (document == null) {
            return null;
        }

        return ContentEntry.builder()
                .id(document.getId())
                .key(document.getKey())
                .type(document.getType())
                .locale(document.getLocale())
                .status(document.getStatus())
                .version(document.getVersion())
                .draftValue(document.getDraftValue())
                .publishedValue(document.getPublishedValue())
                .description(document.getDescription())
                .screen(document.getScreen())
                .legalSlug(document.getLegalSlug())
                .requiresUserAcceptance(document.isRequiresUserAcceptance())
                .effectiveFrom(document.getEffectiveFrom())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .publishedAt(document.getPublishedAt())
                .updatedBy(document.getUpdatedBy())
                .publishedBy(document.getPublishedBy())
                .build();
    }

    public static ContentEntryDocument toDocument(ContentEntry domain) {
        if (domain == null) {
            return null;
        }

        return ContentEntryDocument.builder()
                .id(domain.getId())
                .key(domain.getKey())
                .type(domain.getType())
                .locale(domain.getLocale())
                .status(domain.getStatus())
                .version(domain.getVersion())
                .draftValue(domain.getDraftValue())
                .publishedValue(domain.getPublishedValue())
                .description(domain.getDescription())
                .screen(domain.getScreen())
                .legalSlug(domain.getLegalSlug())
                .requiresUserAcceptance(domain.isRequiresUserAcceptance())
                .effectiveFrom(domain.getEffectiveFrom())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .publishedAt(domain.getPublishedAt())
                .updatedBy(domain.getUpdatedBy())
                .publishedBy(domain.getPublishedBy())
                .build();
    }
}
