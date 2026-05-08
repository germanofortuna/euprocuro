package com.euprocuro.api.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.euprocuro.api.infrastructure.persistence.document.UserBlockListEntryDocument;

public interface SpringDataUserBlockListRepository extends MongoRepository<UserBlockListEntryDocument, String> {
    Optional<UserBlockListEntryDocument> findByDocumentHash(String documentHash);

    Optional<UserBlockListEntryDocument> findByDocumentHashAndActiveTrue(String documentHash);
}
