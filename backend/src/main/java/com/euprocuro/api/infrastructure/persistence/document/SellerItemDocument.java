package com.euprocuro.api.infrastructure.persistence.document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.euprocuro.api.domain.model.InterestCategory;
import com.euprocuro.api.domain.model.LocationInfo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("seller_items")
public class SellerItemDocument {
    @Id
    private String id;
    private String ownerId;
    private String ownerName;
    private String title;
    private String description;
    private String referenceImageUrl;
    private InterestCategory category;
    private BigDecimal desiredPrice;
    private LocationInfo location;
    private List<String> tags;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
