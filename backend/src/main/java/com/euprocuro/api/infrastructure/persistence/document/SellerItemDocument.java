package com.euprocuro.api.infrastructure.persistence.document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

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
@CompoundIndexes({
        @CompoundIndex(name = "seller_item_owner_active_created", def = "{'ownerId': 1, 'active': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "seller_item_active_category_city", def = "{'active': 1, 'category': 1, 'location.city': 1, 'createdAt': -1}")
})
public class SellerItemDocument {
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
    private BigDecimal desiredPrice;
    private LocationInfo location;
    @TextIndexed(weight = 3)
    private List<String> tags;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
