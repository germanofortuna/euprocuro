package com.euprocuro.api.domain.gateway;

import java.util.Optional;

import com.euprocuro.api.domain.model.PaymentOrder;

public interface PaymentOrderGateway {
    PaymentOrder save(PaymentOrder paymentOrder);

    Optional<PaymentOrder> findById(String paymentOrderId);
}
