package com.ecobook.dto

data class AuthResponseDTO(
    val id: String,
    val email: String,
    val nome: String,
    val perfilCompleto: Boolean,
    val role: String,
    val token: String,
    val expiresIn: Long
)
