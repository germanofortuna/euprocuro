package com.euprocuro.api.infrastructure.persistence.document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.euprocuro.api.domain.model.OfferStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document("offers")
public class OfferDocument {
    @Id
    private String id;
    private String interestPostId;
    private String sellerId;
    private String sellerName;
    private String sellerEmail;
    private String sellerPhone;
    private BigDecimal offeredPrice;
    private String message;
    private boolean includesDelivery;
    private List<String> highlights;
    private OfferStatus status;
    private Instant createdAt;
}
