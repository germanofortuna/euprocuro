package com.euprocuro.api.infrastructure.persistence.mapper;

import com.euprocuro.api.domain.model.SellerItem;
import com.euprocuro.api.infrastructure.persistence.document.SellerItemDocument;

public final class SellerItemPersistenceMapper {
    private SellerItemPersistenceMapper() {
    }

    public static SellerItem toDomain(SellerItemDocument document) {
        if (document == null) {
            return null;
        }

        return SellerItem.builder()
                .id(document.getId())
                .ownerId(document.getOwnerId())
                .ownerName(document.getOwnerName())
                .title(document.getTitle())
                .description(document.getDescription())
                .referenceImageUrl(document.getReferenceImageUrl())
                .category(document.getCategory())
                .desiredPrice(document.getDesiredPrice())
                .location(document.getLocation())
                .tags(document.getTags())
                .active(document.isActive())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    public static SellerItemDocument toDocument(SellerItem domain) {
        if (domain == null) {
            return null;
        }

        return SellerItemDocument.builder()
                .id(domain.getId())
                .ownerId(domain.getOwnerId())
                .ownerName(domain.getOwnerName())
                .title(domain.getTitle())
                .description(domain.getDescription())
                .referenceImageUrl(domain.getReferenceImageUrl())
                .category(domain.getCategory())
                .desiredPrice(domain.getDesiredPrice())
                .location(domain.getLocation())
                .tags(domain.getTags())
                .active(domain.isActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
