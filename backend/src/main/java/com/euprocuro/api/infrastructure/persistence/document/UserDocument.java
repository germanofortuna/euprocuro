package com.euprocuro.api.infrastructure.persistence.document;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document("users")
public class UserDocument {
    @Id
    private String id;
    private String name;
    @Indexed(unique = true)
    private String email;
    @Indexed(unique = true, sparse = true)
    private String documentNumber;
    private String documentType;
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
