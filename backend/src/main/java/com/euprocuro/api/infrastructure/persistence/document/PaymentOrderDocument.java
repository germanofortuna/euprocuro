package com.euprocuro.api.infrastructure.persistence.document;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.euprocuro.api.domain.model.PaymentOrderStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document("payment_orders")
public class PaymentOrderDocument {
    @Id
    private String id;
    @Indexed
    private String userId;
    private String userEmail;
    private String productCode;
    private String productName;
    private BigDecimal amount;
    private String paymentMethod;
    private String provider;
    private PaymentOrderStatus status;
    private String providerPreferenceId;
    @Indexed
    private String providerPaymentId;
    private String checkoutUrl;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant approvedAt;
}
