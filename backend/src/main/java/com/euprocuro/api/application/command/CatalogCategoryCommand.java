package com.euprocuro.api.application.command;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CatalogCategoryCommand {
    String code;
    String label;
    boolean active;
    Integer sortOrder;
}
