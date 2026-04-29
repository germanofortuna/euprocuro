package com.euprocuro.api.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.euprocuro.api.application.view.MonetizationProductView;
import com.euprocuro.api.shared.config.MonetizationCatalogProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class MonetizationCatalog {

    private final MonetizationCatalogProperties properties;

    List<MonetizationProductView> products() {
        return properties.toProducts();
    }

    Optional<MonetizationProductView> findByCode(String code) {
        return products().stream()
                .filter(product -> product.getCode().equalsIgnoreCase(code))
                .findFirst();
    }
}
