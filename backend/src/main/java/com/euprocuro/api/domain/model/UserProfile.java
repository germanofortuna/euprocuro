package com.euprocuro.api.domain.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    private String id;
    private String name;
    private String email;
    private String documentNumber;
    private String documentType;
    private String passwordHash;
    private String googleSubject;
    private String facebookSubject;
    private String phone;
    private boolean phoneVerified;
    private String postalCode;
    private String city;
    private String state;
    private String neighborhood;
    private String country;
    private boolean emailVerified;
    private double buyerRating;
    private double sellerRating;
    private Integer sellerCredits;
    private Integer purchasedCreditsTotal;
    private Boolean freeCreditsGranted;
    private String subscriptionPlan;
    private Instant subscriptionActiveUntil;
    private String ipAddress;
    private boolean termsAccepted;
    private Instant termsAcceptedAt;
    private String termsVersion;
}
