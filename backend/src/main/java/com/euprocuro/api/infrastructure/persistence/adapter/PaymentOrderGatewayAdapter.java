package com.euprocuro.api.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.euprocuro.api.domain.gateway.PaymentOrderGateway;
import com.euprocuro.api.domain.model.PaymentOrder;
import com.euprocuro.api.infrastructure.persistence.mapper.PaymentOrderPersistenceMapper;
import com.euprocuro.api.infrastructure.persistence.repository.SpringDataPaymentOrderRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentOrderGatewayAdapter implements PaymentOrderGateway {

    private final SpringDataPaymentOrderRepository repository;

    @Override
    public PaymentOrder save(PaymentOrder paymentOrder) {
        return PaymentOrderPersistenceMapper.toDomain(
                repository.save(PaymentOrderPersistenceMapper.toDocument(paymentOrder))
        );
    }

    @Override
    public Optional<PaymentOrder> findById(String paymentOrderId) {
        return repository.findById(paymentOrderId).map(PaymentOrderPersistenceMapper::toDomain);
    }
}
