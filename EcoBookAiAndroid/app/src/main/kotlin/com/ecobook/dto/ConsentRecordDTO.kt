package com.ecobook.dto

data class ConsentRecordDTO(
    val id: String,
    val consentType: String,
    val status: String,
    val createdAt: String? = null,
    val revokedAt: String? = null
)
