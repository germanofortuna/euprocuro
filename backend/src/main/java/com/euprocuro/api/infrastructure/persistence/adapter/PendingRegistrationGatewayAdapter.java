package com.euprocuro.api.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.euprocuro.api.domain.gateway.PendingRegistrationGateway;
import com.euprocuro.api.domain.model.PendingRegistration;
import com.euprocuro.api.infrastructure.persistence.mapper.PendingRegistrationPersistenceMapper;
import com.euprocuro.api.infrastructure.persistence.repository.SpringDataPendingRegistrationRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PendingRegistrationGatewayAdapter implements PendingRegistrationGateway {

    private final SpringDataPendingRegistrationRepository repository;

    @Override
    public PendingRegistration save(PendingRegistration pendingRegistration) {
        return PendingRegistrationPersistenceMapper.toDomain(
                repository.save(PendingRegistrationPersistenceMapper.toDocument(pendingRegistration)));
    }

    @Override
    public Optional<PendingRegistration> findByEmail(String email) {
        return repository.findByEmail(email).map(PendingRegistrationPersistenceMapper::toDomain);
    }

    @Override
    public void deleteByEmail(String email) {
        repository.deleteByEmail(email);
    }

    @Override
    public void deleteByPhone(String phone) {
        repository.deleteByPhone(phone);
    }
}
