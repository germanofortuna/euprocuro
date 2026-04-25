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
    private String passwordHash;
    private String city;
    private String state;
    private String bio;
    private double buyerRating;
    private double sellerRating;
    private Integer sellerCredits;
    private Integer purchasedCreditsTotal;
    private String subscriptionPlan;
    private Instant subscriptionActiveUntil;
}
