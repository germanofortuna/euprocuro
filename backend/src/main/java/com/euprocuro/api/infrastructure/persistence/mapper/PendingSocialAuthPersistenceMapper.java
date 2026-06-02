package com.euprocuro.api.infrastructure.persistence.mapper;

import com.euprocuro.api.domain.model.PendingSocialAuth;
import com.euprocuro.api.infrastructure.persistence.document.PendingSocialAuthDocument;

public final class PendingSocialAuthPersistenceMapper {

    private PendingSocialAuthPersistenceMapper() {
    }

    public static PendingSocialAuthDocument toDocument(PendingSocialAuth domain) {
        return PendingSocialAuthDocument.builder()
                .id(domain.getId())
                .token(domain.getToken())
                .provider(domain.getProvider())
                .subject(domain.getSubject())
                .email(domain.getEmail())
                .name(domain.getName())
                .existingUserId(domain.getExistingUserId())
                .ipAddress(domain.getIpAddress())
                .createdAt(domain.getCreatedAt())
                .expiresAt(domain.getExpiresAt())
                .build();
    }

    public static PendingSocialAuth toDomain(PendingSocialAuthDocument document) {
        return PendingSocialAuth.builder()
                .id(document.getId())
                .token(document.getToken())
                .provider(document.getProvider())
                .subject(document.getSubject())
                .email(document.getEmail())
                .name(document.getName())
                .existingUserId(document.getExistingUserId())
                .ipAddress(document.getIpAddress())
                .createdAt(document.getCreatedAt())
                .expiresAt(document.getExpiresAt())
                .build();
    }
}
