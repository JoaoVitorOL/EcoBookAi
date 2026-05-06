package com.ecobook.dto

data class GeminiResponseDTO(
    val statusIa: String = "FAILURE",
    val uploadId: String = "",
    val bestPrediction: Map<String, PredictionFieldDTO> = emptyMap(),
    val errorDetails: PreviewErrorDetailsDTO = PreviewErrorDetailsDTO()
)
