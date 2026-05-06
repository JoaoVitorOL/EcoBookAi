package com.ecobook.dto

data class PreviewErrorDetailsDTO(
    val timeout: Boolean = false,
    val malformedResponse: Boolean = false,
    val missingFields: List<String> = emptyList(),
    val invalidEnums: List<String> = emptyList(),
    val message: String? = null
)
