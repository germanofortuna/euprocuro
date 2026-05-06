package com.euprocuro.api.infrastructure.persistence.document;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document("content_revisions")
public class ContentRevisionDocument {
    @Id
    private String id;

    @Indexed
    private String contentEntryId;

    private String key;
    private String locale;
    private int version;
    private String snapshotValue;
    private String publishedBy;
    private Instant publishedAt;
}
