package com.euprocuro.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.euprocuro.api.application.exception.BusinessException;
import com.euprocuro.api.application.exception.ResourceNotFoundException;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class AddressLookupServiceTest {

    @Mock
    private PublicCacheService publicCacheService;

    private AddressLookupService service;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        service = new AddressLookupService(new RestTemplateBuilder(), publicCacheService);
        ReflectionTestUtils.setField(service, "viaCepBaseUrl", "https://viacep.test/ws");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).build();

        lenient().when(publicCacheService.getOrLoad(eq(PublicCacheService.ADDRESS), any(), eq(2_592_000L), any()))
                .thenAnswer(invocation -> invocation.getArgument(3, Supplier.class).get());
    }

    @Test
    void lookupBrazilianPostalCodeShouldNormalizeAndReturnAddress() {
        server.expect(requestTo("https://viacep.test/ws/99709164/json/"))
                .andRespond(withSuccess(
                        "{"
                                + "\"cep\":\"99709-164\","
                                + "\"bairro\":\"Centro\","
                                + "\"localidade\":\"Erechim\","
                                + "\"uf\":\"RS\""
                                + "}",
                        MediaType.APPLICATION_JSON));

        var result = service.lookupBrazilianPostalCode("99709-164");

        assertThat(result.getPostalCode()).isEqualTo("99709-164");
        assertThat(result.getCity()).isEqualTo("Erechim");
        assertThat(result.getState()).isEqualTo("RS");
        assertThat(result.getNeighborhood()).isEqualTo("Centro");
        assertThat(result.getCountry()).isEqualTo("Brasil");
        server.verify();
    }

    @Test
    void lookupBrazilianPostalCodeShouldRejectInvalidPostalCode() {
        assertThatThrownBy(() -> service.lookupBrazilianPostalCode("123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CEP valido");
    }

    @Test
    void lookupBrazilianPostalCodeShouldRejectMissingOrInvalidViaCepResponse() {
        server.expect(requestTo("https://viacep.test/ws/99709164/json/"))
                .andRespond(withSuccess("{\"erro\": true}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.lookupBrazilianPostalCode("99709164"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("CEP nao encontrado");
    }

    @Test
    void lookupBrazilianPostalCodeShouldWrapProviderFailures() {
        server.expect(requestTo("https://viacep.test/ws/99709164/json/"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> service.lookupBrazilianPostalCode("99709164"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("consultar o CEP");
    }
}
