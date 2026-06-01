package com.euprocuro.api.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.euprocuro.api.infrastructure.persistence.document.PendingRegistrationDocument;

public interface SpringDataPendingRegistrationRepository extends MongoRepository<PendingRegistrationDocument, String> {
    Optional<PendingRegistrationDocument> findByEmail(String email);

    void deleteByEmail(String email);

    void deleteByPhone(String phone);
}
