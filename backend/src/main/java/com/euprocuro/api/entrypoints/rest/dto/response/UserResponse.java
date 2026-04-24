package com.euprocuro.api.entrypoints.rest.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserResponse {
    String id;
    String name;
    String email;
    String city;
    String state;
    String bio;
    double buyerRating;
    double sellerRating;
}
