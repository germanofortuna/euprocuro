package com.euprocuro.api.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.euprocuro.api.infrastructure.persistence.document.PasswordResetTokenDocument;

public interface SpringDataPasswordResetTokenRepository extends MongoRepository<PasswordResetTokenDocument, String> {
    Optional<PasswordResetTokenDocument> findByToken(String token);
}
