package com.euprocuro.api.infrastructure.persistence.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.euprocuro.api.infrastructure.persistence.document.ExternalIntegrationLogDocument;

public interface SpringDataExternalIntegrationLogRepository extends MongoRepository<ExternalIntegrationLogDocument, String> {
}
