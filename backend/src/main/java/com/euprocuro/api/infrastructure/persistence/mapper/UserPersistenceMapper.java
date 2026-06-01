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
                .documentNumber(document.getDocumentNumber())
                .documentType(document.getDocumentType())
                .passwordHash(document.getPasswordHash())
                .googleSubject(document.getGoogleSubject())
                .facebookSubject(document.getFacebookSubject())
                .phone(document.getPhone())
                .phoneVerified(Boolean.TRUE.equals(document.getPhoneVerified()))
                .postalCode(document.getPostalCode())
                .city(document.getCity())
                .state(document.getState())
                .neighborhood(document.getNeighborhood())
                .country(document.getCountry())
                .emailVerified(document.getEmailVerified() == null || document.getEmailVerified())
                .buyerRating(document.getBuyerRating())
                .sellerRating(document.getSellerRating())
                .sellerCredits(document.getSellerCredits())
                .purchasedCreditsTotal(document.getPurchasedCreditsTotal())
                .freeCreditsGranted(document.getFreeCreditsGranted())
                .subscriptionPlan(document.getSubscriptionPlan())
                .subscriptionActiveUntil(document.getSubscriptionActiveUntil())
                .ipAddress(document.getIpAddress())
                .termsAccepted(Boolean.TRUE.equals(document.getTermsAccepted()))
                .termsAcceptedAt(document.getTermsAcceptedAt())
                .termsVersion(document.getTermsVersion())
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
                .documentNumber(domain.getDocumentNumber())
                .documentType(domain.getDocumentType())
                .passwordHash(domain.getPasswordHash())
                .googleSubject(domain.getGoogleSubject())
                .facebookSubject(domain.getFacebookSubject())
                .phone(domain.getPhone())
                .phoneVerified(domain.isPhoneVerified())
                .postalCode(domain.getPostalCode())
                .city(domain.getCity())
                .state(domain.getState())
                .neighborhood(domain.getNeighborhood())
                .country(domain.getCountry())
                .emailVerified(domain.isEmailVerified())
                .buyerRating(domain.getBuyerRating())
                .sellerRating(domain.getSellerRating())
                .sellerCredits(domain.getSellerCredits())
                .purchasedCreditsTotal(domain.getPurchasedCreditsTotal())
                .freeCreditsGranted(domain.getFreeCreditsGranted())
                .subscriptionPlan(domain.getSubscriptionPlan())
                .subscriptionActiveUntil(domain.getSubscriptionActiveUntil())
                .ipAddress(domain.getIpAddress())
                .termsAccepted(domain.isTermsAccepted())
                .termsAcceptedAt(domain.getTermsAcceptedAt())
                .termsVersion(domain.getTermsVersion())
                .build();
    }
}
