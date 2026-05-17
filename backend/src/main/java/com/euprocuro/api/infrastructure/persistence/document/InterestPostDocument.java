package com.euprocuro.api.infrastructure.persistence.document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

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
@CompoundIndexes({
        @CompoundIndex(name = "interest_public_city_category_created", def = "{'status': 1, 'location.city': 1, 'category': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "interest_public_boost_created", def = "{'status': 1, 'boostedUntil': -1, 'createdAt': -1}"),
        @CompoundIndex(name = "interest_owner_created", def = "{'ownerId': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "interest_expiration", def = "{'expiresAt': 1, 'createdAt': 1}")
})
public class InterestPostDocument {
    @Id
    private String id;
    private String ownerId;
    private String ownerName;
    @TextIndexed(weight = 5)
    private String title;
    @TextIndexed(weight = 2)
    private String description;
    private String referenceImageUrl;
    private String category;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private LocationDocument location;
    @TextIndexed(weight = 3)
    private List<String> tags;
    private StickerDetailsDocument stickerDetails;
    private Integer desiredRadiusKm;
    private boolean allowsWhatsappContact;
    private String whatsappContact;
    private Instant boostedUntil;
    private String preferredCondition;
    private String preferredContactMode;
    private InterestStatus status;
    private InterestModerationDocument moderation;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant expiresAt;
}
