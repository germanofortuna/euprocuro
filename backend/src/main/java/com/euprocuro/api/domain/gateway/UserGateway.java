package com.euprocuro.api.domain.gateway;

import java.util.List;
import java.util.Optional;

import com.euprocuro.api.domain.model.UserProfile;

public interface UserGateway {
    UserProfile save(UserProfile userProfile);

    List<UserProfile> findAll();

    Optional<UserProfile> findById(String id);

    Optional<UserProfile> findByEmail(String email);

    Optional<UserProfile> findByDocumentNumber(String documentNumber);

    long count();
}
