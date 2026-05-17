package com.euprocuro.api.infrastructure.persistence.mapper;

import java.util.ArrayList;

import com.euprocuro.api.domain.model.InterestModeration;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.LocationInfo;
import com.euprocuro.api.domain.model.StickerDetails;
import com.euprocuro.api.infrastructure.persistence.document.InterestModerationDocument;
import com.euprocuro.api.infrastructure.persistence.document.InterestPostDocument;
import com.euprocuro.api.infrastructure.persistence.document.LocationDocument;
import com.euprocuro.api.infrastructure.persistence.document.StickerDetailsDocument;

public final class InterestPersistenceMapper {

    private InterestPersistenceMapper() {
    }

    public static InterestPost toDomain(InterestPostDocument document) {
        if (document == null) {
            return null;
        }

        return InterestPost.builder()
                .id(document.getId())
                .ownerId(document.getOwnerId())
                .ownerName(document.getOwnerName())
                .title(document.getTitle())
                .description(document.getDescription())
                .referenceImageUrl(document.getReferenceImageUrl())
                .category(document.getCategory())
                .budgetMin(document.getBudgetMin())
                .budgetMax(document.getBudgetMax())
                .location(toDomain(document.getLocation()))
                .tags(document.getTags() == null ? new ArrayList<>() : document.getTags())
                .stickerDetails(toDomain(document.getStickerDetails()))
                .desiredRadiusKm(document.getDesiredRadiusKm())
                .allowsWhatsappContact(document.isAllowsWhatsappContact())
                .whatsappContact(document.getWhatsappContact())
                .boostedUntil(document.getBoostedUntil())
                .preferredCondition(document.getPreferredCondition())
                .preferredContactMode(document.getPreferredContactMode())
                .status(document.getStatus())
                .moderation(toDomain(document.getModeration()))
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .expiresAt(document.getExpiresAt())
                .build();
    }

    public static InterestPostDocument toDocument(InterestPost domain) {
        if (domain == null) {
            return null;
        }

        return InterestPostDocument.builder()
                .id(domain.getId())
                .ownerId(domain.getOwnerId())
                .ownerName(domain.getOwnerName())
                .title(domain.getTitle())
                .description(domain.getDescription())
                .referenceImageUrl(domain.getReferenceImageUrl())
                .category(domain.getCategory())
                .budgetMin(domain.getBudgetMin())
                .budgetMax(domain.getBudgetMax())
                .location(toDocument(domain.getLocation()))
                .tags(domain.getTags())
                .stickerDetails(toDocument(domain.getStickerDetails()))
                .desiredRadiusKm(domain.getDesiredRadiusKm())
                .allowsWhatsappContact(domain.isAllowsWhatsappContact())
                .whatsappContact(domain.getWhatsappContact())
                .boostedUntil(domain.getBoostedUntil())
                .preferredCondition(domain.getPreferredCondition())
                .preferredContactMode(domain.getPreferredContactMode())
                .status(domain.getStatus())
                .moderation(toDocument(domain.getModeration()))
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .expiresAt(domain.getExpiresAt())
                .build();
    }

    private static InterestModeration toDomain(InterestModerationDocument document) {
        if (document == null) {
            return null;
        }

        return InterestModeration.builder()
                .riskLevel(document.getRiskLevel())
                .categories(document.getCategories())
                .scores(document.getScores())
                .reviewRequired(document.isReviewRequired())
                .provider(document.getProvider())
                .reason(document.getReason())
                .checkedAt(document.getCheckedAt())
                .reviewedBy(document.getReviewedBy())
                .reviewedAt(document.getReviewedAt())
                .build();
    }

    private static InterestModerationDocument toDocument(InterestModeration domain) {
        if (domain == null) {
            return null;
        }

        return InterestModerationDocument.builder()
                .riskLevel(domain.getRiskLevel())
                .categories(domain.getCategories())
                .scores(domain.getScores())
                .reviewRequired(domain.isReviewRequired())
                .provider(domain.getProvider())
                .reason(domain.getReason())
                .checkedAt(domain.getCheckedAt())
                .reviewedBy(domain.getReviewedBy())
                .reviewedAt(domain.getReviewedAt())
                .build();
    }

    private static LocationInfo toDomain(LocationDocument document) {
        if (document == null) {
            return null;
        }

        return LocationInfo.builder()
                .postalCode(document.getPostalCode())
                .city(document.getCity())
                .state(document.getState())
                .neighborhood(document.getNeighborhood())
                .country(document.getCountry())
                .remote(document.isRemote())
                .build();
    }

    private static LocationDocument toDocument(LocationInfo domain) {
        if (domain == null) {
            return null;
        }

        return LocationDocument.builder()
                .postalCode(domain.getPostalCode())
                .city(domain.getCity())
                .state(domain.getState())
                .neighborhood(domain.getNeighborhood())
                .country(domain.getCountry())
                .remote(domain.isRemote())
                .build();
    }

    private static StickerDetails toDomain(StickerDetailsDocument document) {
        if (document == null) {
            return null;
        }

        return StickerDetails.builder()
                .type(document.getType())
                .group(document.getGroup())
                .selection(document.getSelection())
                .numbers(document.getNumbers() == null ? new ArrayList<>() : document.getNumbers())
                .build();
    }

    private static StickerDetailsDocument toDocument(StickerDetails domain) {
        if (domain == null) {
            return null;
        }

        return StickerDetailsDocument.builder()
                .type(domain.getType())
                .group(domain.getGroup())
                .selection(domain.getSelection())
                .numbers(domain.getNumbers())
                .build();
    }
}
