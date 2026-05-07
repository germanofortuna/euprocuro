package com.euprocuro.api.entrypoints.rest.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.euprocuro.api.domain.model.OmbudsmanRequestStatus;

import lombok.Data;

@Data
public class RespondOmbudsmanRequest {
    @NotBlank
    @Size(max = 2000)
    private String adminResponse;

    private OmbudsmanRequestStatus status;
}
