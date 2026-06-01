package com.euprocuro.api.domain.gateway;

import java.util.Optional;

import com.euprocuro.api.domain.model.PendingRegistration;

public interface PendingRegistrationGateway {
    PendingRegistration save(PendingRegistration pendingRegistration);

    Optional<PendingRegistration> findByEmail(String email);

    void deleteByEmail(String email);

    void deleteByPhone(String phone);
}
