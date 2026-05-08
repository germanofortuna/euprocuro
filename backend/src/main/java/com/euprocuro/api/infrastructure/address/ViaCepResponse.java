package com.euprocuro.api.infrastructure.address;

import lombok.Data;

@Data
public class ViaCepResponse {
    private String cep;
    private String bairro;
    private String localidade;
    private String uf;
    private boolean erro;
}
