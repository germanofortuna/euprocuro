package com.euprocuro.api.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.euprocuro.api.infrastructure.persistence.document.ContentRevisionDocument;

public interface SpringDataContentRevisionRepository extends MongoRepository<ContentRevisionDocument, String> {
    List<ContentRevisionDocument> findByContentEntryIdOrderByVersionDesc(String contentEntryId);
}
