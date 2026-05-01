package com.euprocuro.api.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ModerationContent {
    String title;
    String description;
    String tags;
    String imageUrl;
}
