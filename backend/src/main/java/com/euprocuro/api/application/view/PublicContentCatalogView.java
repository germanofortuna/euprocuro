package com.euprocuro.api.application.view;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PublicContentCatalogView {
    String locale;
    String version;
    List<ContentEntryView> entries;
}
