package com.euprocuro.api.entrypoints.rest.dto.request;

import lombok.Data;

@Data
public class SaveOperationalFlagsRequest {
    private MonetizationSettingsRequest monetizationSettings = new MonetizationSettingsRequest();
    private ModerationSettingsRequest moderationSettings = new ModerationSettingsRequest();
    private FeatureFlagsRequest featureFlags = new FeatureFlagsRequest();
    private OperationalFieldsRequest operationalFields = new OperationalFieldsRequest();

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
    public static class FeatureFlagsRequest {
        private Boolean stickersPageEnabled;
        private Boolean sellerProPlanEnabled;
        private Boolean captchaEnabled;
    }

    @Data
    public static class OperationalFieldsRequest {
        private Integer initialFreeCredits;
        private Integer listingRenewalCredits;
    }
}
