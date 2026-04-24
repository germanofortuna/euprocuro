package com.euprocuro.api.application.view;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PasswordResetRequestView {
    String message;
    String previewResetLink;
    String previewToken;
}
