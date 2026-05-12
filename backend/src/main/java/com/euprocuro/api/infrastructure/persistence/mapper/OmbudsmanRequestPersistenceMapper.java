package com.euprocuro.api.infrastructure.persistence.mapper;

import com.euprocuro.api.domain.model.OmbudsmanRequest;
import com.euprocuro.api.infrastructure.persistence.document.OmbudsmanRequestDocument;

public final class OmbudsmanRequestPersistenceMapper {

    private OmbudsmanRequestPersistenceMapper() {
    }

    public static OmbudsmanRequest toDomain(OmbudsmanRequestDocument document) {
        if (document == null) {
            return null;
        }

        return OmbudsmanRequest.builder()
                .id(document.getId())
                .protocol(document.getProtocol())
                .userId(document.getUserId())
                .name(document.getName())
                .email(document.getEmail())
                .type(document.getType())
                .subject(document.getSubject())
                .message(document.getMessage())
                .relatedEntityType(document.getRelatedEntityType())
                .relatedEntityId(document.getRelatedEntityId())
                .status(document.getStatus())
                .adminResponse(document.getAdminResponse())
                .answeredBy(document.getAnsweredBy())
                .answeredAt(document.getAnsweredAt())
                .closedAt(document.getClosedAt())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    public static OmbudsmanRequestDocument toDocument(OmbudsmanRequest domain) {
        if (domain == null) {
            return null;
        }

        return OmbudsmanRequestDocument.builder()
                .id(domain.getId())
                .protocol(domain.getProtocol())
                .userId(domain.getUserId())
                .name(domain.getName())
                .email(domain.getEmail())
                .type(domain.getType())
                .subject(domain.getSubject())
                .message(domain.getMessage())
                .relatedEntityType(domain.getRelatedEntityType())
                .relatedEntityId(domain.getRelatedEntityId())
                .status(domain.getStatus())
                .adminResponse(domain.getAdminResponse())
                .answeredBy(domain.getAnsweredBy())
                .answeredAt(domain.getAnsweredAt())
                .closedAt(domain.getClosedAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
