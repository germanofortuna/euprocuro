package com.euprocuro.api.entrypoints.rest.dto.request;

import lombok.Data;

@Data
public class SaveOperationalFlagsRequest {
    private MonetizationSettingsRequest monetizationSettings = new MonetizationSettingsRequest();
    private ModerationSettingsRequest moderationSettings = new ModerationSettingsRequest();

    @Data
    public static class MonetizationSettingsRequest {
        private boolean creditPurchasesEnabled;
        private boolean boostPurchasesEnabled;
    }

    @Data
    public static class ModerationSettingsRequest {
        private boolean userBlockListEnabled = true;
    }
}
