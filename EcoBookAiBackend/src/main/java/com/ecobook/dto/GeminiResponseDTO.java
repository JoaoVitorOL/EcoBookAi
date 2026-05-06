package com.ecobook.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class GeminiResponseDTO {

    private String statusIa;

    @Builder.Default
    private String uploadId = "";

    @Builder.Default
    private Map<String, PredictionFieldDTO> bestPrediction = new LinkedHashMap<>();

    @Builder.Default
    private PreviewErrorDetailsDTO errorDetails = new PreviewErrorDetailsDTO();
}
