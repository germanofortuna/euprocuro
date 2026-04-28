package com.euprocuro.api.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.euprocuro.api.domain.gateway.InterestGateway;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.InterestSearchCriteria;
import com.euprocuro.api.domain.model.InterestStatus;
import com.euprocuro.api.infrastructure.persistence.document.InterestPostDocument;
import com.euprocuro.api.infrastructure.persistence.mapper.InterestPersistenceMapper;
import com.euprocuro.api.infrastructure.persistence.repository.SpringDataInterestRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InterestGatewayAdapter implements InterestGateway {

    private final SpringDataInterestRepository repository;
    private final MongoTemplate mongoTemplate;

    @Override
    public InterestPost save(InterestPost interestPost) {
        return InterestPersistenceMapper.toDomain(repository.save(InterestPersistenceMapper.toDocument(interestPost)));
    }

    @Override
    public List<InterestPost> findAll() {
        return repository.findAll()
                .stream()
                .map(InterestPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InterestPost> search(InterestSearchCriteria criteria, int offset, int limit) {
        int safeOffset = Math.max(0, offset);
        int safeLimit = Math.max(1, limit);
        Instant now = Instant.now();
        List<InterestPostDocument> documents = new ArrayList<>();

        Query activeBoostQuery = new Query(activeBoostCriteria(criteria, now))
                .with(Sort.by(Sort.Direction.DESC, "createdAt"));
        long activeBoostCount = mongoTemplate.count(activeBoostQuery, InterestPostDocument.class);

        if (safeOffset < activeBoostCount) {
            Query activePageQuery = new Query(activeBoostCriteria(criteria, now))
                    .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                    .skip(safeOffset)
                    .limit(safeLimit);
            documents.addAll(mongoTemplate.find(activePageQuery, InterestPostDocument.class));
        }

        if (documents.size() < safeLimit) {
            long regularOffset = Math.max(0, safeOffset - activeBoostCount);
            Query regularPageQuery = new Query(regularInterestCriteria(criteria, now))
                    .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                    .skip(regularOffset)
                    .limit(safeLimit - documents.size());
            documents.addAll(mongoTemplate.find(regularPageQuery, InterestPostDocument.class));
        }

        return documents.stream()
                .map(InterestPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InterestPost> findByOwnerIdOrderByCreatedAtDesc(String ownerId) {
        return repository.findByOwnerIdOrderByCreatedAtDesc(ownerId)
                .stream()
                .map(InterestPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InterestPost> findByStatusOrderByCreatedAtDesc(InterestStatus status) {
        return repository.findByStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(InterestPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<InterestPost> findById(String id) {
        return repository.findById(id).map(InterestPersistenceMapper::toDomain);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    @Override
    public long countByStatus(InterestStatus status) {
        return repository.countByStatus(status);
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public long deleteExpired(Instant now, Instant legacyCutoff) {
        Query query = new Query(new Criteria().orOperator(
                Criteria.where("expiresAt").lt(now),
                new Criteria().andOperator(
                        new Criteria().orOperator(
                                Criteria.where("expiresAt").exists(false),
                                Criteria.where("expiresAt").is(null)
                        ),
                        Criteria.where("createdAt").lt(legacyCutoff)
                )
        ));
        return mongoTemplate.remove(query, InterestPostDocument.class).getDeletedCount();
    }

    private Criteria activeBoostCriteria(InterestSearchCriteria criteria, Instant now) {
        return new Criteria().andOperator(
                baseCriteria(criteria, now),
                Criteria.where("boostEnabled").is(true),
                Criteria.where("boostedUntil").gt(now)
        );
    }

    private Criteria regularInterestCriteria(InterestSearchCriteria criteria, Instant now) {
        return new Criteria().andOperator(
                baseCriteria(criteria, now),
                new Criteria().orOperator(
                        Criteria.where("boostEnabled").is(false),
                        Criteria.where("boostedUntil").exists(false),
                        Criteria.where("boostedUntil").is(null),
                        Criteria.where("boostedUntil").lte(now)
                )
        );
    }

    private Criteria baseCriteria(InterestSearchCriteria searchCriteria, Instant now) {
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(new Criteria().orOperator(
                Criteria.where("expiresAt").exists(false),
                Criteria.where("expiresAt").is(null),
                Criteria.where("expiresAt").gt(now)
        ));

        if (searchCriteria.isOpenOnly()) {
            criteria.add(Criteria.where("status").is(InterestStatus.OPEN));
        }

        if (searchCriteria.getCategory() != null) {
            criteria.add(Criteria.where("category").is(searchCriteria.getCategory()));
        }

        if (searchCriteria.getCity() != null && !searchCriteria.getCity().isBlank()) {
            criteria.add(Criteria.where("location.city")
                    .regex(exactRegex(searchCriteria.getCity()), "i"));
        }

        if (searchCriteria.getMaxBudget() != null) {
            criteria.add(new Criteria().orOperator(
                    Criteria.where("budgetMax").exists(false),
                    Criteria.where("budgetMax").is(null),
                    Criteria.where("budgetMax").lte(searchCriteria.getMaxBudget())
            ));
        }

        if (searchCriteria.getQuery() != null && !searchCriteria.getQuery().isBlank()) {
            String queryRegex = containsRegex(searchCriteria.getQuery());
            criteria.add(new Criteria().orOperator(
                    Criteria.where("title").regex(queryRegex, "i"),
                    Criteria.where("description").regex(queryRegex, "i"),
                    Criteria.where("tags").regex(queryRegex, "i")
            ));
        }

        return criteria.isEmpty()
                ? new Criteria()
                : new Criteria().andOperator(criteria.toArray(new Criteria[0]));
    }

    private String exactRegex(String value) {
        return "^" + Pattern.quote(value.trim()) + "$";
    }

    private String containsRegex(String value) {
        return ".*" + Pattern.quote(value.trim()) + ".*";
    }
}
