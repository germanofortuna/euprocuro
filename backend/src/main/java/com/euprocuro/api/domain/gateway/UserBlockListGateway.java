package com.euprocuro.api.domain.gateway;

import java.util.Optional;

import com.euprocuro.api.domain.model.UserBlockListEntry;

public interface UserBlockListGateway {
    UserBlockListEntry save(UserBlockListEntry entry);

    Optional<UserBlockListEntry> findByDocumentHash(String documentHash);

    Optional<UserBlockListEntry> findByDocumentHashAndActiveTrue(String documentHash);
}
