package com.euprocuro.api.infrastructure.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class LocationDocument {
    private String city;
    private String state;
    private String neighborhood;
    private boolean remote;
}
