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
public class PasswordResetToken {
    private String id;
    private String token;
    private String userId;
    private Instant expiresAt;
    private Instant usedAt;
    private Instant createdAt;
}
