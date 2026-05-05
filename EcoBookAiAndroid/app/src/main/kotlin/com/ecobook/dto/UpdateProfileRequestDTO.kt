package com.ecobook.dto

data class UpdateProfileRequestDTO(
    val nome: String,
    val whatsapp: String,
    val cidade: String,
    val bairro: String,
    val instituicao: String? = null,
    val consentimentoIa: Boolean,
    val necessidadesAcademicas: Set<String>
)
