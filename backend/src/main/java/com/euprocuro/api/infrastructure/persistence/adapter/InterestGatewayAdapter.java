package com.euprocuro.api.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.euprocuro.api.domain.gateway.InterestGateway;
import com.euprocuro.api.domain.model.InterestPost;
import com.euprocuro.api.domain.model.InterestStatus;
import com.euprocuro.api.infrastructure.persistence.mapper.InterestPersistenceMapper;
import com.euprocuro.api.infrastructure.persistence.repository.SpringDataInterestRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InterestGatewayAdapter implements InterestGateway {

    private final SpringDataInterestRepository repository;

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
    public long countByStatus(InterestStatus status) {
        return repository.countByStatus(status);
    }

    @Override
    public long count() {
        return repository.count();
    }
}
