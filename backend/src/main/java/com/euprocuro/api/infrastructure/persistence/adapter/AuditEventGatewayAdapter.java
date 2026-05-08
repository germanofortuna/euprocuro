package com.euprocuro.api.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;

import com.euprocuro.api.domain.gateway.AuditEventGateway;
import com.euprocuro.api.domain.model.AuditEvent;
import com.euprocuro.api.infrastructure.persistence.mapper.AuditEventPersistenceMapper;
import com.euprocuro.api.infrastructure.persistence.repository.SpringDataAuditEventRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuditEventGatewayAdapter implements AuditEventGateway {

    private final SpringDataAuditEventRepository repository;

    @Override
    public AuditEvent save(AuditEvent auditEvent) {
        return AuditEventPersistenceMapper.toDomain(repository.save(AuditEventPersistenceMapper.toDocument(auditEvent)));
    }
}
