package com.euprocuro.api.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.euprocuro.api.domain.gateway.ContentEntryGateway;
import com.euprocuro.api.domain.model.ContentEntry;
import com.euprocuro.api.domain.model.ContentEntryStatus;
import com.euprocuro.api.infrastructure.persistence.mapper.ContentEntryPersistenceMapper;
import com.euprocuro.api.infrastructure.persistence.repository.SpringDataContentEntryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ContentEntryGatewayAdapter implements ContentEntryGateway {

    private final SpringDataContentEntryRepository repository;

    @Override
    public ContentEntry save(ContentEntry entry) {
        return ContentEntryPersistenceMapper.toDomain(repository.save(ContentEntryPersistenceMapper.toDocument(entry)));
    }

    @Override
    public List<ContentEntry> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "screen", "key"))
                .stream()
                .map(ContentEntryPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ContentEntry> findByStatusAndLocale(ContentEntryStatus status, String locale) {
        return repository.findByStatusAndLocaleOrderByKeyAsc(status, locale)
                .stream()
                .map(ContentEntryPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ContentEntry> findByStatusAndLocaleAndKeyIn(
            ContentEntryStatus status,
            String locale,
            Collection<String> keys
    ) {
        return repository.findByStatusAndLocaleAndKeyInOrderByKeyAsc(status, locale, keys)
                .stream()
                .map(ContentEntryPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ContentEntry> findById(String id) {
        return repository.findById(id).map(ContentEntryPersistenceMapper::toDomain);
    }

    @Override
    public Optional<ContentEntry> findByKeyAndLocale(String key, String locale) {
        return repository.findByKeyAndLocale(key, locale).map(ContentEntryPersistenceMapper::toDomain);
    }
}
