package com.euprocuro.api.infrastructure.persistence.mapper;

import java.util.ArrayList;

import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.LocationInfo;
import com.euprocuro.api.infrastructure.persistence.document.InterestPostDocument;
import com.euprocuro.api.infrastructure.persistence.document.LocationDocument;

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
                .desiredRadiusKm(document.getDesiredRadiusKm())
                .acceptsNationwideOffers(document.isAcceptsNationwideOffers())
                .boostEnabled(document.isBoostEnabled())
                .boostedUntil(document.getBoostedUntil())
                .preferredCondition(document.getPreferredCondition())
                .preferredContactMode(document.getPreferredContactMode())
                .status(document.getStatus())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
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
                .desiredRadiusKm(domain.getDesiredRadiusKm())
                .acceptsNationwideOffers(domain.isAcceptsNationwideOffers())
                .boostEnabled(domain.isBoostEnabled())
                .boostedUntil(domain.getBoostedUntil())
                .preferredCondition(domain.getPreferredCondition())
                .preferredContactMode(domain.getPreferredContactMode())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private static LocationInfo toDomain(LocationDocument document) {
        if (document == null) {
            return null;
        }

        return LocationInfo.builder()
                .city(document.getCity())
                .state(document.getState())
                .neighborhood(document.getNeighborhood())
                .remote(document.isRemote())
                .build();
    }

    private static LocationDocument toDocument(LocationInfo domain) {
        if (domain == null) {
            return null;
        }

        return LocationDocument.builder()
                .city(domain.getCity())
                .state(domain.getState())
                .neighborhood(domain.getNeighborhood())
                .remote(domain.isRemote())
                .build();
    }
}
