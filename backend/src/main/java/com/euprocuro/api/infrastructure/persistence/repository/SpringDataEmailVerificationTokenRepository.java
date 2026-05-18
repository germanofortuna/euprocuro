package com.euprocuro.api.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.euprocuro.api.infrastructure.persistence.document.EmailVerificationTokenDocument;

public interface SpringDataEmailVerificationTokenRepository extends MongoRepository<EmailVerificationTokenDocument, String> {
    Optional<EmailVerificationTokenDocument> findByToken(String token);

    void deleteByUserId(String userId);
}
