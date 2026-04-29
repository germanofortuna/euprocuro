package com.euprocuro.api.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrder {
    private String id;
    private String userId;
    private String userEmail;
    private String productCode;
    private String productName;
    private BigDecimal amount;
    private String paymentMethod;
    private String provider;
    private PaymentOrderStatus status;
    private String providerPreferenceId;
    private String providerPaymentId;
    private String checkoutUrl;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant approvedAt;
}
