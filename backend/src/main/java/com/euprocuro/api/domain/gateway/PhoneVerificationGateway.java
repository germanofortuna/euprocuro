package com.euprocuro.api.domain.gateway;

import com.euprocuro.api.domain.model.PhoneVerificationChannel;

public interface PhoneVerificationGateway {

    /**
     * Inicia uma verificacao de telefone enviando um codigo pelo canal informado.
     * A geracao, expiracao e contagem de tentativas do codigo ficam a cargo do provedor.
     */
    void startVerification(String phoneE164, PhoneVerificationChannel channel);

    /**
     * Confere o codigo informado pelo usuario para o telefone.
     *
     * @return true quando o codigo e valido (aprovado pelo provedor).
     */
    boolean checkVerification(String phoneE164, String code);
}
