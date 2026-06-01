package com.euprocuro.api.entrypoints.rest.dto.response;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MeResponse {
    private String id;
    private String name;
    private String email;
    private String phone;
    private boolean phoneVerified;
    private String postalCode;
    private String city;
    private String state;
    private String neighborhood;
    private String country;
    private Integer credits;
    private Instant expiresAt;
    private boolean admin;
}
