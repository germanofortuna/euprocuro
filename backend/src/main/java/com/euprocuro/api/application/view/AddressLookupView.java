package com.euprocuro.api.application.view;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AddressLookupView {
    String postalCode;
    String city;
    String state;
    String neighborhood;
    String country;
}
