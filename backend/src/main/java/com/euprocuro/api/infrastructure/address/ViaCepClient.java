package com.euprocuro.api.infrastructure.address;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "viaCepClient",
        url = "${application.address.lookup.viacep-base-url:https://viacep.com.br/ws}"
)
public interface ViaCepClient {

    @GetMapping("/{postalCode}/json/")
    ViaCepResponse lookup(@PathVariable("postalCode") String postalCode);
}
