package com.euprocuro.api.entrypoints.rest.dto.request;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;

import com.euprocuro.api.domain.model.MonetizationProductType;

import lombok.Data;

@Data
public class SaveOperationalCatalogRequest {
    private MonetizationSettingsRequest monetizationSettings = new MonetizationSettingsRequest();
    private ModerationSettingsRequest moderationSettings = new ModerationSettingsRequest();

    @Valid
    private List<CategoryRequest> categories = new ArrayList<>();

    @Valid
    private List<ProductRequest> products = new ArrayList<>();

    @Data
    public static class MonetizationSettingsRequest {
        private boolean creditPurchasesEnabled;
        private boolean boostPurchasesEnabled;
    }

    @Data
    public static class ModerationSettingsRequest {
        private boolean userBlockListEnabled = true;
    }

    @Data
    public static class CategoryRequest {
        private String code;
        private String value;
        private String label;
        private boolean active;
        private Integer sortOrder;
    }

    @Data
    public static class ProductRequest {
        private String code;
        private String name;
        private String description;
        private MonetizationProductType type;
        private BigDecimal price;
        private BigDecimal originalPrice;
        private boolean promotional;
        private String promotionLabel;
        private Integer credits;
        private Integer durationDays;
        private boolean enabled;
        private Integer sortOrder;
    }
}
