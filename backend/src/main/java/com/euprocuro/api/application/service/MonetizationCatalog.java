package com.euprocuro.api.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.euprocuro.api.application.view.MonetizationProductView;
import com.euprocuro.api.application.view.MonetizationSettingsView;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class MonetizationCatalog {

    private final OperationalCatalogService operationalCatalogService;

    List<MonetizationProductView> products() {
        return operationalCatalogService.listActiveProducts();
    }

    MonetizationSettingsView settings() {
        return operationalCatalogService.getMonetizationSettings();
    }

    boolean creditPurchasesEnabled() {
        return settings().isCreditPurchasesEnabled();
    }

    boolean boostPurchasesEnabled() {
        return settings().isBoostPurchasesEnabled();
    }

    Optional<MonetizationProductView> findByCode(String code) {
        return products().stream()
                .filter(product -> product.getCode().equalsIgnoreCase(code))
                .findFirst();
    }
}
