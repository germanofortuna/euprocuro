package com.euprocuro.api.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.euprocuro.api.infrastructure.persistence.document.OfferDocument;

public interface SpringDataOfferRepository extends MongoRepository<OfferDocument, String> {
    List<OfferDocument> findByInterestPostIdOrderByCreatedAtDesc(String interestPostId);

    List<OfferDocument> findByInterestPostIdInOrderByCreatedAtDesc(List<String> interestPostIds);

    List<OfferDocument> findBySellerIdOrderByCreatedAtDesc(String sellerId);
}
