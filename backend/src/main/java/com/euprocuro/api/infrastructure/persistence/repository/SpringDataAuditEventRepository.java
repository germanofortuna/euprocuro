package com.euprocuro.api.infrastructure.persistence.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.euprocuro.api.infrastructure.persistence.document.AuditEventDocument;

public interface SpringDataAuditEventRepository extends MongoRepository<AuditEventDocument, String> {
}
