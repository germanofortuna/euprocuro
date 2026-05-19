package com.euprocuro.api.application.view;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GoogleIdentityView {
    private String subject;
    private String email;
    private String name;
    private boolean emailVerified;
}
