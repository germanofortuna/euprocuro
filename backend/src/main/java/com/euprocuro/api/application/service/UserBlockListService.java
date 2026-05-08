package com.euprocuro.api.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.euprocuro.api.application.view.ModerationSettingsView;
import com.euprocuro.api.domain.gateway.UserBlockListGateway;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.UserBlockListEntry;
import com.euprocuro.api.domain.model.UserProfile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserBlockListService {

    private final UserBlockListGateway userBlockListGateway;
    private final OperationalCatalogService operationalCatalogService;

    @Value("${application.security.document-hash-pepper:}")
    private String documentHashPepper;

    public boolean isEnabled() {
        ModerationSettingsView settings = operationalCatalogService.getModerationSettings();
        return settings == null || settings.isUserBlockListEnabled();
    }

    public Optional<UserBlockListEntry> findActiveBlock(UserProfile user) {
        if (!isEnabled()) {
            return Optional.empty();
        }

        String documentNumber = normalizeDocument(user == null ? null : user.getDocumentNumber());
        if (!StringUtils.hasText(documentNumber)) {
            return Optional.empty();
        }
        return userBlockListGateway.findByDocumentHashAndActiveTrue(documentHash(documentNumber));
    }

    public Optional<UserBlockListEntry> block(UserProfile user, InterestPost interest, String sourceProvider, String reason) {
        if (!isEnabled()) {
            return Optional.empty();
        }

        String documentNumber = normalizeDocument(user == null ? null : user.getDocumentNumber());
        if (!StringUtils.hasText(documentNumber)) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        String documentHash = documentHash(documentNumber);
        UserBlockListEntry existing = userBlockListGateway.findByDocumentHash(documentHash).orElse(null);

        assert user != null;
        UserBlockListEntry entry = UserBlockListEntry.builder()
                .id(existing == null ? null : existing.getId())
                .userId(user.getId())
                .userEmail(user.getEmail())
                .documentHash(documentHash)
                .documentLast4(last4(documentNumber))
                .documentType(StringUtils.hasText(user.getDocumentType()) ? user.getDocumentType() : documentType(documentNumber))
                .active(true)
                .sourceProvider(sourceProvider)
                .sourceInterestId(interest == null ? null : interest.getId())
                .reason(reason)
                .occurrenceCount(existing == null ? 1 : existing.getOccurrenceCount() + 1)
                .firstBlockedAt(existing == null || existing.getFirstBlockedAt() == null ? now : existing.getFirstBlockedAt())
                .lastBlockedAt(now)
                .createdAt(existing == null || existing.getCreatedAt() == null ? now : existing.getCreatedAt())
                .updatedAt(now)
                .build();
        return Optional.of(userBlockListGateway.save(entry));
    }

    private String normalizeDocument(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private String documentType(String documentNumber) {
        return documentNumber.length() == 11 ? "CPF" : "CNPJ";
    }

    private String last4(String documentNumber) {
        return documentNumber.length() <= 4 ? documentNumber : documentNumber.substring(documentNumber.length() - 4);
    }

    private String documentHash(String documentNumber) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((Optional.ofNullable(documentHashPepper).orElse("") + ":" + documentNumber)
                    .getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Algoritmo de hash indisponivel.", exception);
        }
    }
}
