package com.euprocuro.api.domain.gateway;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.euprocuro.api.domain.model.ContentEntry;
import com.euprocuro.api.domain.model.ContentEntryStatus;

public interface ContentEntryGateway {
    ContentEntry save(ContentEntry entry);

    List<ContentEntry> findAll();

    List<ContentEntry> findByStatusAndLocale(ContentEntryStatus status, String locale);

    List<ContentEntry> findByStatusAndLocaleAndKeyIn(ContentEntryStatus status, String locale, Collection<String> keys);

    Optional<ContentEntry> findById(String id);

    Optional<ContentEntry> findByKeyAndLocale(String key, String locale);
}
