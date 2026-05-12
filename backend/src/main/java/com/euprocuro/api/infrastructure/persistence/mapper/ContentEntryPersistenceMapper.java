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
                .defaultValue(document.getDefaultValue())
                .defaultValueHash(document.getDefaultValueHash())
                .description(document.getDescription())
                .screen(document.getScreen())
                .legalSlug(document.getLegalSlug())
                .requiresUserAcceptance(document.isRequiresUserAcceptance())
                .defaultUpdateAvailable(document.isDefaultUpdateAvailable())
                .ignoredDefaultValueHash(document.getIgnoredDefaultValueHash())
                .effectiveFrom(document.getEffectiveFrom())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .defaultUpdatedAt(document.getDefaultUpdatedAt())
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
                .defaultValue(domain.getDefaultValue())
                .defaultValueHash(domain.getDefaultValueHash())
                .description(domain.getDescription())
                .screen(domain.getScreen())
                .legalSlug(domain.getLegalSlug())
                .requiresUserAcceptance(domain.isRequiresUserAcceptance())
                .defaultUpdateAvailable(domain.isDefaultUpdateAvailable())
                .ignoredDefaultValueHash(domain.getIgnoredDefaultValueHash())
                .effectiveFrom(domain.getEffectiveFrom())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .defaultUpdatedAt(domain.getDefaultUpdatedAt())
                .publishedAt(domain.getPublishedAt())
                .updatedBy(domain.getUpdatedBy())
                .publishedBy(domain.getPublishedBy())
                .build();
    }
}
