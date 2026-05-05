package com.euprocuro.api.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.euprocuro.api.domain.model.ContentEntryStatus;
import com.euprocuro.api.infrastructure.persistence.document.ContentEntryDocument;

public interface SpringDataContentEntryRepository extends MongoRepository<ContentEntryDocument, String> {
    List<ContentEntryDocument> findByStatusAndLocaleOrderByKeyAsc(ContentEntryStatus status, String locale);

    List<ContentEntryDocument> findByStatusAndLocaleAndKeyInOrderByKeyAsc(
            ContentEntryStatus status,
            String locale,
            Collection<String> keys
    );

    Optional<ContentEntryDocument> findByKeyAndLocale(String key, String locale);
}
