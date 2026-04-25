package com.euprocuro.api.application.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.euprocuro.api.application.view.MonetizationProductView;
import com.euprocuro.api.domain.model.MonetizationProductType;

final class MonetizationCatalog {

    private static final List<MonetizationProductView> PRODUCTS = List.of(
            MonetizationProductView.builder()
                    .code("CREDITS_10")
                    .name("10 propostas")
                    .description("Pacote para vendedores enviarem propostas avulsas.")
                    .type(MonetizationProductType.CREDIT_PACK)
                    .price(new BigDecimal("9.90"))
                    .credits(10)
                    .build(),
            MonetizationProductView.builder()
                    .code("CREDITS_30")
                    .name("30 propostas")
                    .description("Mais volume para vendedores frequentes.")
                    .type(MonetizationProductType.CREDIT_PACK)
                    .price(new BigDecimal("24.90"))
                    .credits(30)
                    .build(),
            MonetizationProductView.builder()
                    .code("SELLER_PRO")
                    .name("Plano vendedor Pro")
                    .description("Propostas ilimitadas por 30 dias neste MVP.")
                    .type(MonetizationProductType.SUBSCRIPTION)
                    .price(new BigDecimal("49.90"))
                    .durationDays(30)
                    .build(),
            MonetizationProductView.builder()
                    .code("BOOST_3_DAYS")
                    .name("Boost 3 dias")
                    .description("Impulsiona o interesse na busca e na home.")
                    .type(MonetizationProductType.BOOST)
                    .price(new BigDecimal("9.90"))
                    .durationDays(3)
                    .build(),
            MonetizationProductView.builder()
                    .code("BOOST_7_DAYS")
                    .name("Boost 7 dias")
                    .description("Mais tempo em destaque para receber propostas.")
                    .type(MonetizationProductType.BOOST)
                    .price(new BigDecimal("19.90"))
                    .durationDays(7)
                    .build()
    );

    private MonetizationCatalog() {
    }

    static List<MonetizationProductView> products() {
        return PRODUCTS;
    }

    static Optional<MonetizationProductView> findByCode(String code) {
        return PRODUCTS.stream()
                .filter(product -> product.getCode().equalsIgnoreCase(code))
                .findFirst();
    }
}
