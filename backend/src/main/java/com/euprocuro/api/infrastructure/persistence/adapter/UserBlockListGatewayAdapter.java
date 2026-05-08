package com.euprocuro.api.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.euprocuro.api.domain.gateway.UserBlockListGateway;
import com.euprocuro.api.domain.model.UserBlockListEntry;
import com.euprocuro.api.infrastructure.persistence.mapper.UserBlockListPersistenceMapper;
import com.euprocuro.api.infrastructure.persistence.repository.SpringDataUserBlockListRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserBlockListGatewayAdapter implements UserBlockListGateway {

    private final SpringDataUserBlockListRepository repository;

    @Override
    public UserBlockListEntry save(UserBlockListEntry entry) {
        return UserBlockListPersistenceMapper.toDomain(repository.save(UserBlockListPersistenceMapper.toDocument(entry)));
    }

    @Override
    public Optional<UserBlockListEntry> findByDocumentHash(String documentHash) {
        return repository.findByDocumentHash(documentHash)
                .map(UserBlockListPersistenceMapper::toDomain);
    }

    @Override
    public Optional<UserBlockListEntry> findByDocumentHashAndActiveTrue(String documentHash) {
        return repository.findByDocumentHashAndActiveTrue(documentHash)
                .map(UserBlockListPersistenceMapper::toDomain);
    }
}
