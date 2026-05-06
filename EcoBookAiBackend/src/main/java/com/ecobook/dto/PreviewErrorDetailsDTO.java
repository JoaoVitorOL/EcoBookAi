package com.ecobook.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PreviewErrorDetailsDTO {

    @Builder.Default
    private boolean timeout = false;

    @Builder.Default
    private boolean malformedResponse = false;

    @Builder.Default
    private List<String> missingFields = new ArrayList<>();

    @Builder.Default
    private List<String> invalidEnums = new ArrayList<>();

    private String message;
}
