package com.euprocuro.api.domain.gateway;

import java.util.Optional;

import com.euprocuro.api.domain.model.EmailVerificationToken;

public interface EmailVerificationTokenGateway {
    EmailVerificationToken save(EmailVerificationToken token);

    Optional<EmailVerificationToken> findByToken(String token);

    void deleteByToken(String token);

    void deleteByUserId(String userId);
}
