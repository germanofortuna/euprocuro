package com.euprocuro.api.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.euprocuro.api.domain.gateway.AuthSessionGateway;
import com.euprocuro.api.domain.model.AuthSession;
import com.euprocuro.api.infrastructure.persistence.mapper.AuthSessionPersistenceMapper;
import com.euprocuro.api.infrastructure.persistence.repository.SpringDataAuthSessionRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthSessionGatewayAdapter implements AuthSessionGateway {

    private final SpringDataAuthSessionRepository repository;

    @Override
    public AuthSession save(AuthSession session) {
        return AuthSessionPersistenceMapper.toDomain(
                repository.save(AuthSessionPersistenceMapper.toDocument(session))
        );
    }

    @Override
    public Optional<AuthSession> findByToken(String token) {
        return repository.findByToken(token).map(AuthSessionPersistenceMapper::toDomain);
    }

    @Override
    public void deleteByToken(String token) {
        repository.deleteByToken(token);
    }
}
