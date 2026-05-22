package com.ecobook.dto

data class UserConsentStatusDTO(
    val platformConsentGiven: Boolean = false,
    val platformConsentGivenAt: String? = null,
    val aiConsentEnabled: Boolean = false,
    val aiConsentGivenAt: String? = null,
    val aiConsentRevokedAt: String? = null,
    val history: List<ConsentRecordDTO> = emptyList()
)
