package com.euprocuro.api.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.euprocuro.api.infrastructure.persistence.document.ModerationRuleDocument;

public interface SpringDataModerationRuleRepository extends MongoRepository<ModerationRuleDocument, String> {
    List<ModerationRuleDocument> findByActiveTrueOrderByUpdatedAtDesc();
}
