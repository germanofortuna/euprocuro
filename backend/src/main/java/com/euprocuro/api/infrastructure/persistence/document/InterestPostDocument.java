package com.euprocuro.api.infrastructure.persistence.document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.euprocuro.api.domain.model.InterestCategory;
import com.euprocuro.api.domain.model.InterestStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document("interest_posts")
public class InterestPostDocument {
    @Id
    private String id;
    private String ownerId;
    private String ownerName;
    private String title;
    private String description;
    private String referenceImageUrl;
    private InterestCategory category;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private LocationDocument location;
    private List<String> tags;
    private Integer desiredRadiusKm;
    private boolean acceptsNationwideOffers;
    private boolean allowsWhatsappContact;
    private String whatsappContact;
    private boolean boostEnabled;
    private Instant boostedUntil;
    private String preferredCondition;
    private String preferredContactMode;
    private InterestStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant expiresAt;
}
