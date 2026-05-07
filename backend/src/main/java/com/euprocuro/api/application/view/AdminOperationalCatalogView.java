package com.euprocuro.api.application.view;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminOperationalCatalogView {
    MonetizationSettingsView monetizationSettings;
    List<CatalogCategoryView> categories;
    List<MonetizationProductView> products;
    Instant updatedAt;
}
