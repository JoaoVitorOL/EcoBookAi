package com.ecobook.dto

data class MaterialNonReceiptReportDTO(
    val id: String,
    val materialId: String,
    val solicitacaoId: String,
    val estudanteId: String,
    val reason: String? = null,
    val status: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val resolvedAt: String? = null
)
