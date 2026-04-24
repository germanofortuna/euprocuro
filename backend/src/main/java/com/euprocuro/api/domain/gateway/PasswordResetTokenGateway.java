package com.euprocuro.api.domain.gateway;

import java.util.Optional;

import com.euprocuro.api.domain.model.PasswordResetToken;

public interface PasswordResetTokenGateway {
    PasswordResetToken save(PasswordResetToken token);

    Optional<PasswordResetToken> findByToken(String token);
}
