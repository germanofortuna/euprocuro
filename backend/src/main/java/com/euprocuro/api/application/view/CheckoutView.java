package com.euprocuro.api.application.view;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CheckoutView {
    String provider;
    String paymentMethod;
    String productCode;
    String paymentOrderId;
    String providerPreferenceId;
    String checkoutUrl;
    String status;
    String message;
}
