package com.euprocuro.api.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.euprocuro.api.infrastructure.persistence.document.PendingSocialAuthDocument;

public interface SpringDataPendingSocialAuthRepository extends MongoRepository<PendingSocialAuthDocument, String> {
    Optional<PendingSocialAuthDocument> findByToken(String token);

    void deleteByToken(String token);
}
