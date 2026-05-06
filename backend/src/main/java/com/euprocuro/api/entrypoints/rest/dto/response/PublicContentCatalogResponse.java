package com.euprocuro.api.entrypoints.rest.dto.response;

import java.util.Map;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PublicContentCatalogResponse {
    String locale;
    String version;
    Map<String, PublicContentEntryResponse> entries;
}
