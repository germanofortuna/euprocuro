package com.euprocuro.api.domain.gateway;

import java.util.Optional;

import com.euprocuro.api.domain.model.PendingSocialAuth;

public interface PendingSocialAuthGateway {
    PendingSocialAuth save(PendingSocialAuth pendingSocialAuth);

    Optional<PendingSocialAuth> findByToken(String token);

    void deleteByToken(String token);
}
