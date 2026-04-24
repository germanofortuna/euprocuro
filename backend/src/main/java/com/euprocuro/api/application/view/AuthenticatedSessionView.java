package com.euprocuro.api.application.view;

import java.time.Instant;

import com.euprocuro.api.domain.model.UserProfile;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthenticatedSessionView {
    String token;
    Instant expiresAt;
    UserProfile user;
}
