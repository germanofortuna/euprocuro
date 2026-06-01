package com.euprocuro.api.domain.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PendingRegistration {
    private String id;
    private String email;
    private String phone;
    private String name;
    private String passwordHash;
    private String termsVersion;
    private String ipAddress;
    private Instant createdAt;
    private Instant expiresAt;
}
