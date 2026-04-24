package com.euprocuro.api.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.euprocuro.api.infrastructure.persistence.document.AuthSessionDocument;

public interface SpringDataAuthSessionRepository extends MongoRepository<AuthSessionDocument, String> {
    Optional<AuthSessionDocument> findByToken(String token);

    void deleteByToken(String token);
}
