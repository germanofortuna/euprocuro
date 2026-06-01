package com.euprocuro.api.infrastructure.persistence.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.euprocuro.api.domain.gateway.UserGateway;

import lombok.RequiredArgsConstructor;

/**
 * Marca usuarios legados (criados antes do credito preguiçoso) com {@code freeCreditsGranted=true},
 * pois eles ja receberam os creditos iniciais na criacao da conta. Evita reconcessao quando esses
 * usuarios verificarem o telefone. Idempotente: so atualiza registros com o campo nulo.
 */
@Component
@RequiredArgsConstructor
public class FreeCreditsGrantBackfillRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeCreditsGrantBackfillRunner.class);

    private final UserGateway userGateway;

    @Override
    public void run(ApplicationArguments args) {
        long updated = userGateway.findAll().stream()
                .filter(user -> user.getFreeCreditsGranted() == null)
                .map(user -> userGateway.save(user.toBuilder().freeCreditsGranted(true).build()))
                .count();

        if (updated > 0) {
            LOGGER.info("Backfill freeCreditsGranted aplicado a {} usuario(s) legado(s).", updated);
        }
    }
}
