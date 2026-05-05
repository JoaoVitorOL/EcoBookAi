package com.ecobook.dto

data class ApiErrorResponseDTO(
    val error: String = "UNKNOWN",
    val message: String = "Unknown error",
    val fieldErrors: Map<String, String> = emptyMap()
)
