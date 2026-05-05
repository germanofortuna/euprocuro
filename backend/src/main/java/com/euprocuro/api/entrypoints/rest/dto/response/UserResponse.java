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
    String city;
    String state;
    boolean emailVerified;
    double buyerRating;
    double sellerRating;
    Integer sellerCredits;
    Integer purchasedCreditsTotal;
    String subscriptionPlan;
    Instant subscriptionActiveUntil;
}
