package com.euprocuro.api.entrypoints.rest.dto.request;

import javax.validation.constraints.NotNull;

import com.euprocuro.api.domain.model.OmbudsmanRequestStatus;

import lombok.Data;

@Data
public class UpdateOmbudsmanStatusRequest {
    @NotNull
    private OmbudsmanRequestStatus status;
}
