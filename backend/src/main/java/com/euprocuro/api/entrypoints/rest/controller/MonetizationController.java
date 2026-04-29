package com.euprocuro.api.entrypoints.rest.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

import com.euprocuro.api.application.usecase.MonetizationUseCase;
import com.euprocuro.api.application.exception.UnauthorizedException;
import com.euprocuro.api.entrypoints.rest.dto.request.BoostInterestRequest;
import com.euprocuro.api.entrypoints.rest.dto.request.PurchaseProductRequest;
import com.euprocuro.api.entrypoints.rest.dto.response.CheckoutResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.InterestResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.MonetizationAccountResponse;
import com.euprocuro.api.entrypoints.rest.dto.response.MonetizationProductResponse;
import com.euprocuro.api.entrypoints.rest.mapper.RestMapper;
import com.euprocuro.api.entrypoints.rest.security.CurrentUserContext;
import com.euprocuro.api.infrastructure.payment.MercadoPagoWebhookSignatureValidator;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/monetization")
@RequiredArgsConstructor
public class MonetizationController {

    private final MonetizationUseCase monetizationUseCase;
    private final MercadoPagoWebhookSignatureValidator mercadoPagoWebhookSignatureValidator;

    @Value("${application.monetization.local-checkout.success-url:http://localhost:5173?payment=success}")
    private String localCheckoutSuccessUrl;

    @GetMapping("/products")
    public List<MonetizationProductResponse> products() {
        return monetizationUseCase.listProducts()
                .stream()
                .map(RestMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/account")
    public MonetizationAccountResponse account(HttpServletRequest request) {
        return RestMapper.toResponse(monetizationUseCase.getAccount(CurrentUserContext.userId(request)));
    }

    @PostMapping("/purchase")
    public CheckoutResponse purchase(
            HttpServletRequest request,
            @Valid @RequestBody PurchaseProductRequest requestBody
    ) {
        return RestMapper.toResponse(
                monetizationUseCase.purchase(CurrentUserContext.userId(request), RestMapper.toCommand(requestBody))
        );
    }

    @PostMapping("/interests/{interestId}/boost")
    public InterestResponse boostInterest(
            @PathVariable String interestId,
            HttpServletRequest request,
            @Valid @RequestBody BoostInterestRequest requestBody
    ) {
        return RestMapper.toResponse(
                monetizationUseCase.boostInterest(
                        CurrentUserContext.userId(request),
                        interestId,
                        RestMapper.toCommand(requestBody)
                )
        );
    }

    @PostMapping("/mercado-pago/webhook")
    public ResponseEntity<Void> mercadoPagoWebhook(
            @RequestParam Map<String, String> queryParams,
            @RequestHeader(value = "x-signature", required = false) String signature,
            @RequestHeader(value = "x-request-id", required = false) String requestId,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        String type = firstText(queryParams.get("type"), queryParams.get("topic"), bodyValue(body, "type"));
        String paymentId = firstText(
                queryParams.get("data.id"),
                queryParams.get("id"),
                nestedBodyValue(body, "data", "id")
        );

        if ("payment".equalsIgnoreCase(type) && StringUtils.hasText(paymentId)) {
            if (!mercadoPagoWebhookSignatureValidator.isValid(paymentId, requestId, signature)) {
                throw new UnauthorizedException("Assinatura do webhook Mercado Pago invalida.");
            }
            monetizationUseCase.confirmPayment(paymentId);
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/local-checkout/approve/{paymentOrderId}")
    public RedirectView approveLocalCheckout(@PathVariable String paymentOrderId) {
        monetizationUseCase.approveLocalCheckout(paymentOrderId);
        return new RedirectView(localCheckoutSuccessUrl);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String bodyValue(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return null;
        }
        return String.valueOf(body.get(key));
    }

    private String nestedBodyValue(Map<String, Object> body, String parentKey, String childKey) {
        if (body == null || !(body.get(parentKey) instanceof Map)) {
            return null;
        }

        Map<?, ?> nested = (Map<?, ?>) body.get(parentKey);
        Object value = nested.get(childKey);
        return value == null ? null : String.valueOf(value);
    }
}
