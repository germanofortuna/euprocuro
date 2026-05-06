package com.euprocuro.api.application.view;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminContentCatalogView {
    List<ContentEntryView> entries;
}
