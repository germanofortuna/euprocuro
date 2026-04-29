package com.euprocuro.api.infrastructure.payment;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.euprocuro.api.application.view.CheckoutView;
import com.euprocuro.api.application.view.MonetizationProductView;
import com.euprocuro.api.domain.gateway.PaymentCheckoutGateway;
import com.euprocuro.api.domain.model.PaymentOrder;
import com.euprocuro.api.domain.model.UserProfile;

@Component
public class MercadoPagoCheckoutGateway implements PaymentCheckoutGateway {

    private static final String CREATE_PREFERENCE_URL = "https://api.mercadopago.com/checkout/preferences";

    private final RestTemplate restTemplate;

    @Value("${application.monetization.mercado-pago.access-token:}")
    private String accessToken;
    @Value("${application.monetization.mercado-pago.sandbox:true}")
    private boolean sandbox;
    @Value("${application.monetization.mercado-pago.success-url:http://localhost:5173?payment=success}")
    private String successUrl;
    @Value("${application.monetization.mercado-pago.failure-url:http://localhost:5173?payment=failure}")
    private String failureUrl;
    @Value("${application.monetization.mercado-pago.pending-url:http://localhost:5173?payment=pending}")
    private String pendingUrl;
    @Value("${application.monetization.mercado-pago.notification-url:}")
    private String notificationUrl;

    public MercadoPagoCheckoutGateway(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @Override
    public CheckoutView createCheckout(UserProfile user, MonetizationProductView product, PaymentOrder paymentOrder) {
        if (!StringUtils.hasText(accessToken)) {
            throw new IllegalStateException("Configure MERCADO_PAGO_ACCESS_TOKEN para usar o Checkout Pro.");
        }

        Map<String, Object> body = buildPreferenceBody(user, product, paymentOrder);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            Map response = restTemplate.exchange(
                    CREATE_PREFERENCE_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class
            ).getBody();

            String preferenceId = valueAsString(response, "id");
            String checkoutUrl = sandbox
                    ? Optional.ofNullable(valueAsString(response, "sandbox_init_point")).orElse(valueAsString(response, "init_point"))
                    : Optional.ofNullable(valueAsString(response, "init_point")).orElse(valueAsString(response, "sandbox_init_point"));

            if (!StringUtils.hasText(preferenceId) || !StringUtils.hasText(checkoutUrl)) {
                throw new IllegalStateException("Mercado Pago nao retornou link de checkout.");
            }

            return CheckoutView.builder()
                    .provider(paymentOrder.getProvider())
                    .paymentMethod(paymentOrder.getPaymentMethod())
                    .productCode(product.getCode())
                    .paymentOrderId(paymentOrder.getId())
                    .providerPreferenceId(preferenceId)
                    .checkoutUrl(checkoutUrl)
                    .status("PENDING")
                    .message("Checkout criado. Finalize o pagamento no Mercado Pago para liberar os creditos.")
                    .build();
        } catch (RestClientException exception) {
            throw new IllegalStateException("Nao foi possivel criar a preferencia no Mercado Pago.", exception);
        }
    }

    private Map<String, Object> buildPreferenceBody(UserProfile user, MonetizationProductView product, PaymentOrder paymentOrder) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", product.getCode());
        item.put("title", product.getName());
        item.put("description", product.getDescription());
        item.put("quantity", 1);
        item.put("currency_id", "BRL");
        item.put("unit_price", Optional.ofNullable(product.getPrice()).orElse(BigDecimal.ZERO));

        Map<String, Object> payer = new LinkedHashMap<>();
        payer.put("name", user.getName());
        payer.put("email", user.getEmail());

        Map<String, Object> backUrls = new LinkedHashMap<>();
        backUrls.put("success", successUrl);
        backUrls.put("failure", failureUrl);
        backUrls.put("pending", pendingUrl);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("payment_order_id", paymentOrder.getId());
        metadata.put("user_id", user.getId());
        metadata.put("product_code", product.getCode());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", List.of(item));
        body.put("payer", payer);
        body.put("back_urls", backUrls);
        body.put("auto_return", "approved");
        body.put("external_reference", paymentOrder.getId());
        body.put("metadata", metadata);

        if (StringUtils.hasText(notificationUrl)) {
            body.put("notification_url", notificationUrl);
        }

        return body;
    }

    private String valueAsString(Map response, String key) {
        if (response == null || response.get(key) == null) {
            return null;
        }

        return String.valueOf(response.get(key));
    }
}
