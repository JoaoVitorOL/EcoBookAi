package com.ecobook.dto

data class HealthResponseDTO(
    val status: String,
    val timestamp: String,
    val application: String,
    val version: String
)
