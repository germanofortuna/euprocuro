package com.euprocuro.api.infrastructure.persistence.document;

import java.util.List;

import com.euprocuro.api.domain.model.StickerListingType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class StickerDetailsDocument {
    private StickerListingType type;
    private String group;
    private String selection;
    private List<String> numbers;
}
