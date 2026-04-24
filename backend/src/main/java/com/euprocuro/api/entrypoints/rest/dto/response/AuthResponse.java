package com.euprocuro.api.entrypoints.rest.dto.response;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthResponse {
    String token;
    Instant expiresAt;
    UserResponse user;
}
