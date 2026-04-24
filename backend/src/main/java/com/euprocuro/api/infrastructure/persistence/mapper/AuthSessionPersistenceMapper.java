package com.euprocuro.api.infrastructure.persistence.mapper;

import com.euprocuro.api.domain.model.AuthSession;
import com.euprocuro.api.infrastructure.persistence.document.AuthSessionDocument;

public final class AuthSessionPersistenceMapper {

    private AuthSessionPersistenceMapper() {
    }

    public static AuthSession toDomain(AuthSessionDocument document) {
        if (document == null) {
            return null;
        }

        return AuthSession.builder()
                .id(document.getId())
                .token(document.getToken())
                .userId(document.getUserId())
                .expiresAt(document.getExpiresAt())
                .createdAt(document.getCreatedAt())
                .build();
    }

    public static AuthSessionDocument toDocument(AuthSession domain) {
        if (domain == null) {
            return null;
        }

        return AuthSessionDocument.builder()
                .id(domain.getId())
                .token(domain.getToken())
                .userId(domain.getUserId())
                .expiresAt(domain.getExpiresAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
