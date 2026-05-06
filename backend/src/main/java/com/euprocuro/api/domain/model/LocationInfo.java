package com.euprocuro.api.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class LocationInfo {
    private String postalCode;
    private String city;
    private String state;
    private String neighborhood;
    private String country;
    private boolean remote;
}
