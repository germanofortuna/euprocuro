package com.euprocuro.api.application.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.exception.ResourceNotFoundException;
import com.euprocuro.api.application.view.AddressLookupView;

import lombok.Data;

@Service
public class AddressLookupService {

    private static final String BRAZIL = "Brasil";

    private final RestTemplate restTemplate;
    private final PublicCacheService publicCacheService;

    @Value("${application.address.lookup.viacep-base-url:https://viacep.com.br/ws}")
    private String viaCepBaseUrl;
    @Value("${application.cache.public.address-ttl-seconds:2592000}")
    private long addressCacheTtlSeconds = 2_592_000;

    public AddressLookupService(RestTemplateBuilder restTemplateBuilder, PublicCacheService publicCacheService) {
        this.publicCacheService = publicCacheService;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
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
        ViaCepResponse response;
        try {
            response = restTemplate.getForObject(
                    viaCepBaseUrl + "/{postalCode}/json/",
                    ViaCepResponse.class,
                    digits
            );
        } catch (RestClientException exception) {
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

    @Data
    private static class ViaCepResponse {
        private String cep;
        private String bairro;
        private String localidade;
        private String uf;
        private boolean erro;
    }
}
