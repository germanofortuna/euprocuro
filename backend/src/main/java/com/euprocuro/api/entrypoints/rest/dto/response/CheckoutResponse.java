package com.euprocuro.api.entrypoints.rest.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CheckoutResponse {
    String provider;
    String paymentMethod;
    String productCode;
    String checkoutUrl;
    String message;
}
