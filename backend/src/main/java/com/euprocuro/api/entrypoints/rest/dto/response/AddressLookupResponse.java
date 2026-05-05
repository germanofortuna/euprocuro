package com.euprocuro.api.entrypoints.rest.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AddressLookupResponse {
    String postalCode;
    String city;
    String state;
    String neighborhood;
    String country;
}
