package com.euprocuro.api.infrastructure.persistence.mapper;

import com.euprocuro.api.domain.model.PaymentOrder;
import com.euprocuro.api.infrastructure.persistence.document.PaymentOrderDocument;

public final class PaymentOrderPersistenceMapper {

    private PaymentOrderPersistenceMapper() {
    }

    public static PaymentOrder toDomain(PaymentOrderDocument document) {
        if (document == null) {
            return null;
        }

        return PaymentOrder.builder()
                .id(document.getId())
                .userId(document.getUserId())
                .userEmail(document.getUserEmail())
                .productCode(document.getProductCode())
                .productName(document.getProductName())
                .amount(document.getAmount())
                .paymentMethod(document.getPaymentMethod())
                .provider(document.getProvider())
                .status(document.getStatus())
                .providerPreferenceId(document.getProviderPreferenceId())
                .providerPaymentId(document.getProviderPaymentId())
                .checkoutUrl(document.getCheckoutUrl())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .approvedAt(document.getApprovedAt())
                .build();
    }

    public static PaymentOrderDocument toDocument(PaymentOrder domain) {
        if (domain == null) {
            return null;
        }

        return PaymentOrderDocument.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .userEmail(domain.getUserEmail())
                .productCode(domain.getProductCode())
                .productName(domain.getProductName())
                .amount(domain.getAmount())
                .paymentMethod(domain.getPaymentMethod())
                .provider(domain.getProvider())
                .status(domain.getStatus())
                .providerPreferenceId(domain.getProviderPreferenceId())
                .providerPaymentId(domain.getProviderPaymentId())
                .checkoutUrl(domain.getCheckoutUrl())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .approvedAt(domain.getApprovedAt())
                .build();
    }
}
