package com.euprocuro.api.application.command;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OperationalFlagsCommand {
    MonetizationSettingsCommand monetizationSettings;
    ModerationSettingsCommand moderationSettings;
    FeatureFlagsCommand featureFlags;
    OperationalFieldsCommand operationalFields;
}
