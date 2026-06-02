package com.euprocuro.api.domain.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cadastro social pendente de verificacao de telefone. Criado quando o usuario conecta
 * Google/Facebook mas ainda precisa confirmar um telefone por SMS antes da conta existir
 * (ou, para conta existente sem telefone verificado, antes de concluir o login).
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PendingSocialAuth {
    private String id;
    private String token;
    private String provider;
    private String subject;
    private String email;
    private String name;
    private String existingUserId;
    private String ipAddress;
    private Instant createdAt;
    private Instant expiresAt;
}
