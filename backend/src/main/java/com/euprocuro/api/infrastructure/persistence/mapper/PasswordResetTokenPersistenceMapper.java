package com.euprocuro.api.infrastructure.persistence.mapper;

import com.euprocuro.api.domain.model.PasswordResetToken;
import com.euprocuro.api.infrastructure.persistence.document.PasswordResetTokenDocument;

public final class PasswordResetTokenPersistenceMapper {

    private PasswordResetTokenPersistenceMapper() {
    }

    public static PasswordResetToken toDomain(PasswordResetTokenDocument document) {
        if (document == null) {
            return null;
        }

        return PasswordResetToken.builder()
                .id(document.getId())
                .token(document.getToken())
                .userId(document.getUserId())
                .expiresAt(document.getExpiresAt())
                .usedAt(document.getUsedAt())
                .createdAt(document.getCreatedAt())
                .build();
    }

    public static PasswordResetTokenDocument toDocument(PasswordResetToken domain) {
        if (domain == null) {
            return null;
        }

        return PasswordResetTokenDocument.builder()
                .id(domain.getId())
                .token(domain.getToken())
                .userId(domain.getUserId())
                .expiresAt(domain.getExpiresAt())
                .usedAt(domain.getUsedAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
