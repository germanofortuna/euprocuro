package com.euprocuro.api.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.euprocuro.api.domain.model.ContentReportStatus;
import com.euprocuro.api.infrastructure.persistence.document.ContentReportDocument;

public interface SpringDataContentReportRepository extends MongoRepository<ContentReportDocument, String> {
    List<ContentReportDocument> findByStatusOrderByCreatedAtDesc(ContentReportStatus status);
}
