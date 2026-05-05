package com.euprocuro.api.application.view;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CatalogCategoryView {
    String code;
    String label;
    boolean active;
    Integer sortOrder;
}
