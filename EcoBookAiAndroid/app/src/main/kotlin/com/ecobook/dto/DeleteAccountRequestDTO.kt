package com.ecobook.dto

data class DeleteAccountRequestDTO(
    val password: String,
    val reason: String? = null
)
