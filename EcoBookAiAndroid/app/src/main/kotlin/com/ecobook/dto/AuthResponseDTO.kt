package com.ecobook.dto

data class AuthResponseDTO(
    val id: String,
    val email: String,
    val nome: String,
    val whatsapp: String? = null,
    val cidade: String? = null,
    val bairro: String? = null,
    val instituicao: String? = null,
    val perfilCompleto: Boolean,
    val consentimentoIa: Boolean = false,
    val role: String,
    val token: String,
    val expiresIn: Long
)
