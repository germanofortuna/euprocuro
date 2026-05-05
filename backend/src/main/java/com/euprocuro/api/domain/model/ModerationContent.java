package com.euprocuro.api.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Value
@Builder
@Data
public class ModerationContent {
    String title;
    String description;
    String tags;
    String imageUrl;
}
