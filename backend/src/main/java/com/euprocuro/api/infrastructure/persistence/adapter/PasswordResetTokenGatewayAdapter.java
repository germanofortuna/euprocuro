package com.euprocuro.api.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.euprocuro.api.domain.gateway.PasswordResetTokenGateway;
import com.euprocuro.api.domain.model.PasswordResetToken;
import com.euprocuro.api.infrastructure.persistence.mapper.PasswordResetTokenPersistenceMapper;
import com.euprocuro.api.infrastructure.persistence.repository.SpringDataPasswordResetTokenRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenGatewayAdapter implements PasswordResetTokenGateway {

    private final SpringDataPasswordResetTokenRepository repository;

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        return PasswordResetTokenPersistenceMapper.toDomain(
                repository.save(PasswordResetTokenPersistenceMapper.toDocument(token))
        );
    }

    @Override
    public Optional<PasswordResetToken> findByToken(String token) {
        return repository.findByToken(token).map(PasswordResetTokenPersistenceMapper::toDomain);
    }
}
