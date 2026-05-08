package com.euprocuro.api.entrypoints.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LocationResponse {
    String postalCode;
    String city;
    String state;
    String neighborhood;
    String country;
    boolean remote;
}
