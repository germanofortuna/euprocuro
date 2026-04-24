package com.euprocuro.api.domain.gateway;

import java.util.List;
import java.util.Optional;

import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.InterestStatus;

public interface InterestGateway {
    InterestPost save(InterestPost interestPost);

    List<InterestPost> findAll();

    List<InterestPost> findByOwnerIdOrderByCreatedAtDesc(String ownerId);

    List<InterestPost> findByStatusOrderByCreatedAtDesc(InterestStatus status);

    Optional<InterestPost> findById(String id);

    long countByStatus(InterestStatus status);

    long count();
}
