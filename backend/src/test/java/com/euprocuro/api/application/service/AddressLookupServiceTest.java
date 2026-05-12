package com.euprocuro.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.exception.ResourceNotFoundException;
import com.euprocuro.api.infrastructure.address.ViaCepClient;
import com.euprocuro.api.infrastructure.address.ViaCepResponse;

@ExtendWith(MockitoExtension.class)
class AddressLookupServiceTest {

    @Mock
    private ViaCepClient viaCepClient;
    @Mock
    private PublicCacheService publicCacheService;
    @Mock
    private ExternalIntegrationLogService externalIntegrationLogService;

    private AddressLookupService service;

    @BeforeEach
    void setUp() {
        service = new AddressLookupService(viaCepClient, publicCacheService, externalIntegrationLogService);
        ReflectionTestUtils.setField(service, "viaCepBaseUrl", "https://viacep.test/ws");

        lenient().when(publicCacheService.getOrLoad(eq(PublicCacheService.ADDRESS), any(), eq(2_592_000L), any()))
                .thenAnswer(invocation -> invocation.getArgument(3, Supplier.class).get());
        lenient().when(externalIntegrationLogService.startedAt()).thenReturn(Instant.parse("2026-05-06T10:00:00Z"));
    }

    @Test
    void lookupBrazilianPostalCodeShouldNormalizeAndReturnAddress() {
        when(viaCepClient.lookup("99709164")).thenReturn(viaCep("99709-164", "Centro", "Erechim", "RS", false));

        var result = service.lookupBrazilianPostalCode("99709-164");

        assertThat(result.getPostalCode()).isEqualTo("99709-164");
        assertThat(result.getCity()).isEqualTo("Erechim");
        assertThat(result.getState()).isEqualTo("RS");
        assertThat(result.getNeighborhood()).isEqualTo("Centro");
        assertThat(result.getCountry()).isEqualTo("Brasil");
    }

    @Test
    void lookupBrazilianPostalCodeShouldRejectInvalidPostalCode() {
        assertThatThrownBy(() -> service.lookupBrazilianPostalCode("123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CEP valido");
    }

    @Test
    void lookupBrazilianPostalCodeShouldRejectMissingOrInvalidViaCepResponse() {
        when(viaCepClient.lookup("99709164")).thenReturn(viaCep("99709-164", null, null, null, true));

        assertThatThrownBy(() -> service.lookupBrazilianPostalCode("99709164"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("CEP nao encontrado");
    }

    @Test
    void lookupBrazilianPostalCodeShouldWrapProviderFailures() {
        when(viaCepClient.lookup("99709164")).thenThrow(new RuntimeException("provider down"));

        assertThatThrownBy(() -> service.lookupBrazilianPostalCode("99709164"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("consultar o CEP");
    }

    private ViaCepResponse viaCep(String cep, String bairro, String localidade, String uf, boolean erro) {
        ViaCepResponse response = new ViaCepResponse();
        response.setCep(cep);
        response.setBairro(bairro);
        response.setLocalidade(localidade);
        response.setUf(uf);
        response.setErro(erro);
        return response;
    }
}
