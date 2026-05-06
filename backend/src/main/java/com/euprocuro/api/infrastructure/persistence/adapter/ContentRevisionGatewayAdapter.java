package com.euprocuro.api.infrastructure.persistence.adapter;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.euprocuro.api.domain.gateway.ContentRevisionGateway;
import com.euprocuro.api.domain.model.ContentRevision;
import com.euprocuro.api.infrastructure.persistence.mapper.ContentRevisionPersistenceMapper;
import com.euprocuro.api.infrastructure.persistence.repository.SpringDataContentRevisionRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ContentRevisionGatewayAdapter implements ContentRevisionGateway {

    private final SpringDataContentRevisionRepository repository;

    @Override
    public ContentRevision save(ContentRevision revision) {
        return ContentRevisionPersistenceMapper.toDomain(repository.save(ContentRevisionPersistenceMapper.toDocument(revision)));
    }

    @Override
    public List<ContentRevision> findByContentEntryId(String contentEntryId) {
        return repository.findByContentEntryIdOrderByVersionDesc(contentEntryId)
                .stream()
                .map(ContentRevisionPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }
}
