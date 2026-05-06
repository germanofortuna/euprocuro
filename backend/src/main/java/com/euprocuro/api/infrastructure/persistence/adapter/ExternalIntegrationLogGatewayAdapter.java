package com.euprocuro.api.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;

import com.euprocuro.api.domain.gateway.ExternalIntegrationLogGateway;
import com.euprocuro.api.domain.model.ExternalIntegrationLog;
import com.euprocuro.api.infrastructure.persistence.mapper.ExternalIntegrationLogPersistenceMapper;
import com.euprocuro.api.infrastructure.persistence.repository.SpringDataExternalIntegrationLogRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ExternalIntegrationLogGatewayAdapter implements ExternalIntegrationLogGateway {

    private final SpringDataExternalIntegrationLogRepository repository;

    @Override
    public ExternalIntegrationLog save(ExternalIntegrationLog integrationLog) {
        return ExternalIntegrationLogPersistenceMapper.toDomain(repository.save(ExternalIntegrationLogPersistenceMapper.toDocument(integrationLog)));
    }
}
