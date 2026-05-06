package com.euprocuro.api.entrypoints.rest.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminContentCatalogResponse {
    List<ContentEntryResponse> entries;
}
