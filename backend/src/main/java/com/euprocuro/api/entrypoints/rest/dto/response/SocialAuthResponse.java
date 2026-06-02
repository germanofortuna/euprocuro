package com.euprocuro.api.entrypoints.rest.dto.response;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

/**
 * Resposta do login social. Quando {@code phoneRequired} for true, o cliente deve abrir o fluxo
 * de verificacao de telefone usando {@code socialToken}; caso contrario, os demais campos trazem
 * a sessao autenticada (mesmo formato de {@link AuthResponse}).
 */
@Value
@Builder
public class SocialAuthResponse {
    boolean phoneRequired;
    String socialToken;
    String token;
    Instant expiresAt;
    UserResponse user;
}
