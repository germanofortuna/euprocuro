package com.euprocuro.api.domain.gateway;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.InterestSearchCriteria;
import com.euprocuro.api.domain.model.InterestStatus;

public interface InterestGateway {
    InterestPost save(InterestPost interestPost);

    List<InterestPost> findAll();

    List<InterestPost> search(InterestSearchCriteria criteria, int offset, int limit);

    List<InterestPost> findByOwnerIdOrderByCreatedAtDesc(String ownerId);

    List<InterestPost> findByStatusOrderByCreatedAtDesc(InterestStatus status);

    Optional<InterestPost> findById(String id);

    void deleteById(String id);

    long countByStatus(InterestStatus status);

    long count();

    long deleteExpired(Instant now, Instant legacyCutoff);
}
