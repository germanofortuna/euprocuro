package com.euprocuro.api.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.euprocuro.api.domain.model.OmbudsmanRequestStatus;
import com.euprocuro.api.infrastructure.persistence.document.OmbudsmanRequestDocument;

public interface SpringDataOmbudsmanRequestRepository extends MongoRepository<OmbudsmanRequestDocument, String> {
    List<OmbudsmanRequestDocument> findAllByOrderByCreatedAtDesc();

    List<OmbudsmanRequestDocument> findByStatusOrderByCreatedAtDesc(OmbudsmanRequestStatus status);
}
