package com.euprocuro.api.entrypoints.rest.dto.response;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ContentRevisionResponse {
    String id;
    String contentEntryId;
    String key;
    String locale;
    int version;
    String snapshotValue;
    Instant publishedAt;
}
