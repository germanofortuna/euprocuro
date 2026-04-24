package com.euprocuro.api.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.euprocuro.api.domain.model.InterestStatus;
import com.euprocuro.api.infrastructure.persistence.document.InterestPostDocument;

public interface SpringDataInterestRepository extends MongoRepository<InterestPostDocument, String> {
    long countByStatus(InterestStatus status);

    List<InterestPostDocument> findByOwnerIdOrderByCreatedAtDesc(String ownerId);

    List<InterestPostDocument> findByStatusOrderByCreatedAtDesc(InterestStatus status);
}
