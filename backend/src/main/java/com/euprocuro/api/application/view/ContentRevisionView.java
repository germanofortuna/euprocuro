package com.euprocuro.api.application.view;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ContentRevisionView {
    String id;
    String contentEntryId;
    String key;
    String locale;
    int version;
    String snapshotValue;
    Instant publishedAt;
}
