package com.euprocuro.api.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.euprocuro.api.domain.gateway.OmbudsmanRequestGateway;
import com.euprocuro.api.domain.model.OmbudsmanRequest;
import com.euprocuro.api.domain.model.OmbudsmanRequestStatus;
import com.euprocuro.api.infrastructure.persistence.mapper.OmbudsmanRequestPersistenceMapper;
import com.euprocuro.api.infrastructure.persistence.repository.SpringDataOmbudsmanRequestRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OmbudsmanRequestGatewayAdapter implements OmbudsmanRequestGateway {

    private final SpringDataOmbudsmanRequestRepository repository;

    @Override
    public OmbudsmanRequest save(OmbudsmanRequest request) {
        return OmbudsmanRequestPersistenceMapper.toDomain(
                repository.save(OmbudsmanRequestPersistenceMapper.toDocument(request))
        );
    }

    @Override
    public List<OmbudsmanRequest> findAllOrderByCreatedAtDesc() {
        return repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(OmbudsmanRequestPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OmbudsmanRequest> findByStatusOrderByCreatedAtDesc(OmbudsmanRequestStatus status) {
        return repository.findByStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(OmbudsmanRequestPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<OmbudsmanRequest> findById(String id) {
        return repository.findById(id).map(OmbudsmanRequestPersistenceMapper::toDomain);
    }
}
