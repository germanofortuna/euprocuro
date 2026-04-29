package com.euprocuro.api.infrastructure.payment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MercadoPagoWebhookSignatureValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";

    @Value("${application.monetization.mercado-pago.webhook-secret:}")
    private String webhookSecret;

    @Value("${application.monetization.mercado-pago.webhook-signature-required:true}")
    private boolean signatureRequired;

    public boolean isValid(String dataId, String requestId, String signatureHeader) {
        if (!signatureRequired && !StringUtils.hasText(webhookSecret)) {
            return true;
        }

        if (!StringUtils.hasText(webhookSecret)
                || !StringUtils.hasText(dataId)
                || !StringUtils.hasText(requestId)
                || !StringUtils.hasText(signatureHeader)) {
            return false;
        }

        Map<String, String> signatureParts = parseSignatureHeader(signatureHeader);
        String timestamp = signatureParts.get("ts");
        String receivedSignature = signatureParts.get("v1");

        if (!StringUtils.hasText(timestamp) || !StringUtils.hasText(receivedSignature)) {
            return false;
        }

        String manifest = "id:" + dataId.toLowerCase(Locale.ROOT)
                + ";request-id:" + requestId
                + ";ts:" + timestamp
                + ";";
        String expectedSignature = hmacSha256Hex(manifest, webhookSecret);
        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                receivedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private Map<String, String> parseSignatureHeader(String signatureHeader) {
        return Arrays.stream(signatureHeader.split(","))
                .map(String::trim)
                .map(part -> part.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1], (current, ignored) -> current));
    }

    private String hmacSha256Hex(String value, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Optional.ofNullable(digest)
                    .map(this::toHex)
                    .orElse("");
        } catch (Exception exception) {
            throw new IllegalStateException("Nao foi possivel validar a assinatura do Mercado Pago.", exception);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
