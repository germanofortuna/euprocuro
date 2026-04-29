package com.euprocuro.api.domain.gateway;

import com.euprocuro.api.domain.model.PaymentProviderStatus;

public interface PaymentStatusGateway {
    PaymentProviderStatus findPayment(String providerPaymentId);
}
