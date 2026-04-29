package com.euprocuro.api.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PaymentProviderStatus {
    String paymentId;
    String status;
    String externalReference;
    String paymentMethod;
}
