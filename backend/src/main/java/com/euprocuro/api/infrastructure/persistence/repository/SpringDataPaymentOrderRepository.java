package com.euprocuro.api.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.euprocuro.api.infrastructure.persistence.document.PaymentOrderDocument;

public interface SpringDataPaymentOrderRepository extends MongoRepository<PaymentOrderDocument, String> {
    List<PaymentOrderDocument> findTop20ByUserIdOrderByCreatedAtDesc(String userId);
}
