package com.euprocuro.api.entrypoints.rest.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.euprocuro.api.application.service.AddressLookupService;
import com.euprocuro.api.entrypoints.rest.dto.response.AddressLookupResponse;
import com.euprocuro.api.entrypoints.rest.mapper.RestMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@Tag(name = "Addresses", description = "Consulta publica de endereco por CEP.")
public class AddressLookupController {

    private final AddressLookupService addressLookupService;

    @GetMapping("/postal-code/{postalCode}")
    public AddressLookupResponse lookupPostalCode(@PathVariable String postalCode) {
        return RestMapper.toResponse(addressLookupService.lookupBrazilianPostalCode(postalCode));
    }
}
