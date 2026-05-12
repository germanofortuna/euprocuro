package com.euprocuro.api.application.service;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.exception.ResourceNotFoundException;
import com.euprocuro.api.application.view.AddressLookupView;
import com.euprocuro.api.infrastructure.address.ViaCepClient;
import com.euprocuro.api.infrastructure.address.ViaCepResponse;

@Service
public class AddressLookupService {

    private static final String BRAZIL = "Brasil";

    private final ViaCepClient viaCepClient;
    private final PublicCacheService publicCacheService;
    private final ExternalIntegrationLogService externalIntegrationLogService;

    @Value("${application.address.lookup.viacep-base-url:https://viacep.com.br/ws}")
    private String viaCepBaseUrl;
    @Value("${application.cache.public.address-ttl-seconds:2592000}")
    private long addressCacheTtlSeconds = 2_592_000;

    public AddressLookupService(
            ViaCepClient viaCepClient,
            PublicCacheService publicCacheService,
            ExternalIntegrationLogService externalIntegrationLogService
    ) {
        this.viaCepClient = viaCepClient;
        this.publicCacheService = publicCacheService;
        this.externalIntegrationLogService = externalIntegrationLogService;
    }

    public AddressLookupView lookupBrazilianPostalCode(String postalCode) {
        String digits = onlyDigits(postalCode);
        if (digits.length() != 8) {
            throw new BusinessException("Informe um CEP valido com 8 digitos.");
        }

        return publicCacheService.getOrLoad(
                PublicCacheService.ADDRESS,
                digits,
                addressCacheTtlSeconds,
                () -> lookupBrazilianPostalCodeUncached(digits)
        );
    }

    private AddressLookupView lookupBrazilianPostalCodeUncached(String digits) {
        Instant startedAt = externalIntegrationLogService.startedAt();
        String url = viaCepBaseUrl + "/" + digits + "/json/";
        ViaCepResponse response;
        try {
            response = viaCepClient.lookup(digits);
            externalIntegrationLogService.recordSuccess(
                    "VIA_CEP",
                    digits,
                    "GET",
                    url,
                    Map.of(),
                    null,
                    200,
                    response,
                    startedAt
            );
        } catch (RuntimeException exception) {
            externalIntegrationLogService.recordFailure(
                    "VIA_CEP",
                    digits,
                    "GET",
                    url,
                    Map.of(),
                    null,
                    null,
                    null,
                    startedAt,
                    exception
            );
            throw new BusinessException("Nao foi possivel consultar o CEP agora. Tente novamente.");
        }

        if (response == null || response.isErro() || !StringUtils.hasText(response.getLocalidade()) || !StringUtils.hasText(response.getUf())) {
            throw new ResourceNotFoundException("CEP nao encontrado.");
        }

        return AddressLookupView.builder()
                .postalCode(formatPostalCode(digits))
                .city(response.getLocalidade())
                .state(response.getUf())
                .neighborhood(response.getBairro())
                .country(BRAZIL)
                .build();
    }

    private String onlyDigits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private String formatPostalCode(String digits) {
        return digits.substring(0, 5) + "-" + digits.substring(5);
    }

}
