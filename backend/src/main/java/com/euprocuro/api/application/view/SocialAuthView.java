package com.euprocuro.api.application.view;

import lombok.Builder;
import lombok.Value;

/**
 * Resultado de um login social: ou ja resulta em sessao autenticada (usuario existente com
 * telefone verificado), ou exige verificacao de telefone antes de concluir, devolvendo um
 * token que referencia o cadastro social pendente.
 */
@Value
@Builder
public class SocialAuthView {
    boolean phoneRequired;
    String socialToken;
    AuthenticatedSessionView session;

    public static SocialAuthView ofSession(AuthenticatedSessionView session) {
        return SocialAuthView.builder().phoneRequired(false).session(session).build();
    }

    public static SocialAuthView ofPhoneRequired(String socialToken) {
        return SocialAuthView.builder().phoneRequired(true).socialToken(socialToken).build();
    }
}
