package com.euprocuro.api.infrastructure.persistence.document;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.euprocuro.api.domain.model.ContentEntryStatus;
import com.euprocuro.api.domain.model.ContentEntryType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document("content_entries")
@CompoundIndexes({
        @CompoundIndex(name = "content_key_locale_unique", def = "{'key': 1, 'locale': 1}", unique = true),
        @CompoundIndex(name = "content_public_locale_status_key", def = "{'locale': 1, 'status': 1, 'key': 1}")
})
public class ContentEntryDocument {
    @Id
    private String id;

    @Indexed
    private String key;

    private ContentEntryType type;

    @Indexed
    private String locale;

    @Indexed
    private ContentEntryStatus status;

    private int version;
    private String draftValue;
    private String publishedValue;
    private String description;
    private String screen;

    @Indexed
    private String legalSlug;

    private boolean requiresUserAcceptance;
    private Instant effectiveFrom;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant publishedAt;
    private String updatedBy;
    private String publishedBy;
}
