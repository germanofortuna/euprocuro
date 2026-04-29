package com.euprocuro.api.application.view;

import java.math.BigDecimal;
import java.time.Instant;

import com.euprocuro.api.domain.model.PaymentOrderStatus;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PaymentOrderView {
    String id;
    String productCode;
    String productName;
    BigDecimal amount;
    String paymentMethod;
    String provider;
    PaymentOrderStatus status;
    String providerPaymentId;
    Instant createdAt;
    Instant updatedAt;
    Instant approvedAt;
}
