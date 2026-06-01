package com.euprocuro.api.infrastructure.persistence.mapper;

import com.euprocuro.api.domain.model.PendingRegistration;
import com.euprocuro.api.infrastructure.persistence.document.PendingRegistrationDocument;

public final class PendingRegistrationPersistenceMapper {

    private PendingRegistrationPersistenceMapper() {
    }

    public static PendingRegistrationDocument toDocument(PendingRegistration domain) {
        return PendingRegistrationDocument.builder()
                .id(domain.getId())
                .email(domain.getEmail())
                .phone(domain.getPhone())
                .name(domain.getName())
                .passwordHash(domain.getPasswordHash())
                .termsVersion(domain.getTermsVersion())
                .ipAddress(domain.getIpAddress())
                .createdAt(domain.getCreatedAt())
                .expiresAt(domain.getExpiresAt())
                .build();
    }

    public static PendingRegistration toDomain(PendingRegistrationDocument document) {
        return PendingRegistration.builder()
                .id(document.getId())
                .email(document.getEmail())
                .phone(document.getPhone())
                .name(document.getName())
                .passwordHash(document.getPasswordHash())
                .termsVersion(document.getTermsVersion())
                .ipAddress(document.getIpAddress())
                .createdAt(document.getCreatedAt())
                .expiresAt(document.getExpiresAt())
                .build();
    }
}
