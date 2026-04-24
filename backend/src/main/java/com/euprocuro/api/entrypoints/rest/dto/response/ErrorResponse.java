package com.euprocuro.api.entrypoints.rest.dto.response;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ErrorResponse {
    Instant timestamp;
    int status;
    String error;
    List<String> details;
}
