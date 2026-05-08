package com.euprocuro.api.entrypoints.rest.dto.request;

import javax.validation.constraints.NotNull;

import com.euprocuro.api.domain.model.ContentReportStatus;

import lombok.Data;

@Data
public class UpdateContentReportStatusRequest {
    @NotNull
    private ContentReportStatus status;
}
