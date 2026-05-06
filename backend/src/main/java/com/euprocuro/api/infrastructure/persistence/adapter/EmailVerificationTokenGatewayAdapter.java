package com.euprocuro.api.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.euprocuro.api.domain.gateway.EmailVerificationTokenGateway;
import com.euprocuro.api.domain.model.EmailVerificationToken;
import com.euprocuro.api.infrastructure.persistence.mapper.EmailVerificationTokenPersistenceMapper;
import com.euprocuro.api.infrastructure.persistence.repository.SpringDataEmailVerificationTokenRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmailVerificationTokenGatewayAdapter implements EmailVerificationTokenGateway {

    private final SpringDataEmailVerificationTokenRepository repository;

    @Override
    public EmailVerificationToken save(EmailVerificationToken token) {
        return EmailVerificationTokenPersistenceMapper.toDomain(
                repository.save(EmailVerificationTokenPersistenceMapper.toDocument(token))
        );
    }

    @Override
    public Optional<EmailVerificationToken> findByToken(String token) {
        return repository.findByToken(token).map(EmailVerificationTokenPersistenceMapper::toDomain);
    }

    @Override
    public void deleteByToken(String token) {
        repository.findByToken(token).ifPresent(repository::delete);
    }
}
