package com.euprocuro.api.infrastructure.persistence.mapper;

import com.euprocuro.api.domain.model.EmailVerificationToken;
import com.euprocuro.api.infrastructure.persistence.document.EmailVerificationTokenDocument;

public final class EmailVerificationTokenPersistenceMapper {

    private EmailVerificationTokenPersistenceMapper() {
    }

    public static EmailVerificationToken toDomain(EmailVerificationTokenDocument document) {
        if (document == null) {
            return null;
        }

        return EmailVerificationToken.builder()
                .id(document.getId())
                .token(document.getToken())
                .userId(document.getUserId())
                .expiresAt(document.getExpiresAt())
                .usedAt(document.getUsedAt())
                .createdAt(document.getCreatedAt())
                .build();
    }

    public static EmailVerificationTokenDocument toDocument(EmailVerificationToken domain) {
        if (domain == null) {
            return null;
        }

        return EmailVerificationTokenDocument.builder()
                .id(domain.getId())
                .token(domain.getToken())
                .userId(domain.getUserId())
                .expiresAt(domain.getExpiresAt())
                .usedAt(domain.getUsedAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
