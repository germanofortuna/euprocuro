package com.euprocuro.api.application.command;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SaveOperationalCatalogCommand {
    List<CatalogCategoryCommand> categories;
    List<CatalogProductCommand> products;
}
