package com.euprocuro.api.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InterestCategory {
    AUTOMOVEIS("Automóveis"),
    IMOVEIS("Imóveis"),
    SERVICOS("Serviços"),
    ELETRONICOS("Eletrônicos"),
    INSTRUMENTOS("Instrumentos"),
    OUTROS("Outros");

    private final String label;
}
