package com.euprocuro.api.entrypoints.rest.dto.response;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserResponse {
    String id;
    String name;
    String email;
    String phone;
    boolean phoneVerified;
    String postalCode;
    String city;
    String state;
    String neighborhood;
    String country;
    boolean emailVerified;
    boolean googleLinked;
    boolean facebookLinked;
    double buyerRating;
    double sellerRating;
    Integer sellerCredits;
    Integer purchasedCreditsTotal;
    String subscriptionPlan;
    Instant subscriptionActiveUntil;
    boolean admin;
}
