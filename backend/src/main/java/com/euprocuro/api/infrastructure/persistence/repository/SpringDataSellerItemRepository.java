package com.euprocuro.api.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.euprocuro.api.infrastructure.persistence.document.SellerItemDocument;

public interface SpringDataSellerItemRepository extends MongoRepository<SellerItemDocument, String> {
    List<SellerItemDocument> findByOwnerIdOrderByCreatedAtDesc(String ownerId);
}
