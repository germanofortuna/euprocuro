package com.euprocuro.api.infrastructure.persistence.mapper;

import com.euprocuro.api.domain.model.UserProfile;
import com.euprocuro.api.infrastructure.persistence.document.UserDocument;

public final class UserPersistenceMapper {

    private UserPersistenceMapper() {
    }

    public static UserProfile toDomain(UserDocument document) {
        if (document == null) {
            return null;
        }

        return UserProfile.builder()
                .id(document.getId())
                .name(document.getName())
                .email(document.getEmail())
                .passwordHash(document.getPasswordHash())
                .city(document.getCity())
                .state(document.getState())
                .bio(document.getBio())
                .buyerRating(document.getBuyerRating())
                .sellerRating(document.getSellerRating())
                .sellerCredits(document.getSellerCredits())
                .purchasedCreditsTotal(document.getPurchasedCreditsTotal())
                .subscriptionPlan(document.getSubscriptionPlan())
                .subscriptionActiveUntil(document.getSubscriptionActiveUntil())
                .build();
    }

    public static UserDocument toDocument(UserProfile domain) {
        if (domain == null) {
            return null;
        }

        return UserDocument.builder()
                .id(domain.getId())
                .name(domain.getName())
                .email(domain.getEmail())
                .passwordHash(domain.getPasswordHash())
                .city(domain.getCity())
                .state(domain.getState())
                .bio(domain.getBio())
                .buyerRating(domain.getBuyerRating())
                .sellerRating(domain.getSellerRating())
                .sellerCredits(domain.getSellerCredits())
                .purchasedCreditsTotal(domain.getPurchasedCreditsTotal())
                .subscriptionPlan(domain.getSubscriptionPlan())
                .subscriptionActiveUntil(domain.getSubscriptionActiveUntil())
                .build();
    }
}
