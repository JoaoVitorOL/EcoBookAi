package com.ecobook.dto

data class UpdateProfileRequestDTO(
    val email: String? = null,
    val nome: String,
    val whatsapp: String,
    val cpf: String,
    val cidade: String,
    val bairro: String,
    val instituicao: String? = null,
    val consentimentoIa: Boolean? = null,
    val necessidadesAcademicas: Set<String>? = null
)
