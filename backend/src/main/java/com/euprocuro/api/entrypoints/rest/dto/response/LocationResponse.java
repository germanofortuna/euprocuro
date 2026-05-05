package com.euprocuro.api.entrypoints.rest.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LocationResponse {
    String postalCode;
    String city;
    String state;
    String neighborhood;
    String country;
    boolean remote;
}
