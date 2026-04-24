package com.euprocuro.api.entrypoints.rest.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LocationResponse {
    String city;
    String state;
    String neighborhood;
    boolean remote;
}
