package com.euprocuro.api.domain.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ContentRevision {
    private String id;
    private String contentEntryId;
    private String key;
    private String locale;
    private int version;
    private String snapshotValue;
    private String publishedBy;
    private Instant publishedAt;
}
