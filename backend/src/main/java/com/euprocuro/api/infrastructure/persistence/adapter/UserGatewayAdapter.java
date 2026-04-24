package com.euprocuro.api.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.euprocuro.api.domain.gateway.UserGateway;
import com.euprocuro.api.domain.model.UserProfile;
import com.euprocuro.api.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.euprocuro.api.infrastructure.persistence.repository.SpringDataUserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserGatewayAdapter implements UserGateway {

    private final SpringDataUserRepository repository;

    @Override
    public UserProfile save(UserProfile userProfile) {
        return UserPersistenceMapper.toDomain(repository.save(UserPersistenceMapper.toDocument(userProfile)));
    }

    @Override
    public List<UserProfile> findAll() {
        return repository.findAll()
                .stream()
                .map(UserPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UserProfile> findById(String id) {
        return repository.findById(id).map(UserPersistenceMapper::toDomain);
    }

    @Override
    public Optional<UserProfile> findByEmail(String email) {
        return repository.findByEmailIgnoreCase(email).map(UserPersistenceMapper::toDomain);
    }

    @Override
    public long count() {
        return repository.count();
    }
}
