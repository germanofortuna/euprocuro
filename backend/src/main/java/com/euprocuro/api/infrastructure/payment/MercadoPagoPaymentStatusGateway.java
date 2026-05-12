package com.euprocuro.api.infrastructure.payment;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.euprocuro.api.application.service.ExternalIntegrationLogService;
import com.euprocuro.api.domain.gateway.PaymentStatusGateway;
import com.euprocuro.api.domain.model.PaymentProviderStatus;

@Component
public class MercadoPagoPaymentStatusGateway implements PaymentStatusGateway {

    private static final String PAYMENT_URL = "https://api.mercadopago.com/v1/payments/{paymentId}";

    private final RestTemplate restTemplate;
    private final ExternalIntegrationLogService externalIntegrationLogService;

    @Value("${application.monetization.mercado-pago.access-token:}")
    private String accessToken;

    public MercadoPagoPaymentStatusGateway(
            RestTemplateBuilder restTemplateBuilder,
            ExternalIntegrationLogService externalIntegrationLogService
    ) {
        this.restTemplate = restTemplateBuilder.build();
        this.externalIntegrationLogService = externalIntegrationLogService;
    }

    @Override
    public PaymentProviderStatus findPayment(String providerPaymentId) {
        if (!StringUtils.hasText(accessToken)) {
            throw new IllegalStateException("Configure MERCADO_PAGO_ACCESS_TOKEN para consultar pagamentos.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        Instant startedAt = externalIntegrationLogService.startedAt();
        try {
            Map response = restTemplate.exchange(
                    PAYMENT_URL,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class,
                    providerPaymentId
            ).getBody();
            externalIntegrationLogService.recordSuccess(
                    "MERCADO_PAGO_FIND_PAYMENT",
                    providerPaymentId,
                    "GET",
                    PAYMENT_URL.replace("{paymentId}", providerPaymentId),
                    Map.of("Authorization", "Bearer " + accessToken),
                    null,
                    200,
                    response,
                    startedAt
            );

            return PaymentProviderStatus.builder()
                    .paymentId(providerPaymentId)
                    .status(valueAsString(response, "status"))
                    .externalReference(valueAsString(response, "external_reference"))
                    .paymentMethod(valueAsString(response, "payment_method_id"))
                    .build();
        } catch (RestClientException exception) {
            externalIntegrationLogService.recordFailure(
                    "MERCADO_PAGO_FIND_PAYMENT",
                    providerPaymentId,
                    "GET",
                    PAYMENT_URL.replace("{paymentId}", providerPaymentId),
                    Map.of("Authorization", "Bearer " + accessToken),
                    null,
                    null,
                    null,
                    startedAt,
                    exception
            );
            throw new IllegalStateException("Nao foi possivel consultar o pagamento no Mercado Pago.", exception);
        }
    }

    private String valueAsString(Map response, String key) {
        if (response == null || response.get(key) == null) {
            return null;
        }

        return String.valueOf(response.get(key));
    }
}
