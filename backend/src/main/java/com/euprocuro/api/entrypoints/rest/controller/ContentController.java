package com.euprocuro.api.entrypoints.rest.controller;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.euprocuro.api.application.usecase.ContentUseCase;
import com.euprocuro.api.entrypoints.rest.dto.response.PublicContentCatalogResponse;
import com.euprocuro.api.entrypoints.rest.mapper.RestMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
@Tag(name = "Content", description = "Catalogo publico de textos publicados da plataforma.")
public class ContentController {

    private final ContentUseCase contentUseCase;

    @GetMapping("/public")
    public PublicContentCatalogResponse getPublishedContent(
            @RequestParam(defaultValue = "pt-BR") String locale,
            @RequestParam(required = false) String keys
    ) {
        List<String> requestedKeys = keys == null || keys.isBlank()
                ? List.of()
                : Arrays.stream(keys.split(","))
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .collect(Collectors.toList());
        return RestMapper.toResponse(contentUseCase.getPublishedContent(locale, requestedKeys));
    }
}
