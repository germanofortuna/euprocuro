package com.euprocuro.api.application.command;

import com.euprocuro.api.domain.model.OmbudsmanRequestStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespondOmbudsmanRequestCommand {
    private String adminResponse;
    private OmbudsmanRequestStatus status;
}
