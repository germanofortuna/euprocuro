package com.euprocuro.api.domain.gateway;

import java.util.List;
import java.util.Optional;

import com.euprocuro.api.domain.model.OmbudsmanRequest;
import com.euprocuro.api.domain.model.OmbudsmanRequestStatus;

public interface OmbudsmanRequestGateway {
    OmbudsmanRequest save(OmbudsmanRequest request);

    List<OmbudsmanRequest> findAllOrderByCreatedAtDesc();

    List<OmbudsmanRequest> findByStatusOrderByCreatedAtDesc(OmbudsmanRequestStatus status);

    Optional<OmbudsmanRequest> findById(String id);
}
