package com.euprocuro.api.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.euprocuro.api.domain.gateway.PendingSocialAuthGateway;
import com.euprocuro.api.domain.model.PendingSocialAuth;
import com.euprocuro.api.infrastructure.persistence.mapper.PendingSocialAuthPersistenceMapper;
import com.euprocuro.api.infrastructure.persistence.repository.SpringDataPendingSocialAuthRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PendingSocialAuthGatewayAdapter implements PendingSocialAuthGateway {

    private final SpringDataPendingSocialAuthRepository repository;

    @Override
    public PendingSocialAuth save(PendingSocialAuth pendingSocialAuth) {
        return PendingSocialAuthPersistenceMapper.toDomain(
                repository.save(PendingSocialAuthPersistenceMapper.toDocument(pendingSocialAuth)));
    }

    @Override
    public Optional<PendingSocialAuth> findByToken(String token) {
        return repository.findByToken(token).map(PendingSocialAuthPersistenceMapper::toDomain);
    }

    @Override
    public void deleteByToken(String token) {
        repository.deleteByToken(token);
    }
}
