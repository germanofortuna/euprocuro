package com.euprocuro.api.application.usecase;

import java.util.List;

import com.euprocuro.api.application.view.PublicContentCatalogView;

public interface ContentUseCase {
    PublicContentCatalogView getPublishedContent(String locale, List<String> keys);
}
