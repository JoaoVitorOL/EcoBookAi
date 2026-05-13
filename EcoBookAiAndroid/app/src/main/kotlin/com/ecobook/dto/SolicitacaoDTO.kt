package com.ecobook.dto

data class SolicitacaoDTO(
    val id: String,
    val materialId: String,
    val estudanteId: String,
    val status: String,
    val contatoDoador: Map<String, String>? = null,
    val criadoEm: String? = null,
    val atualizadoEm: String? = null,
    val aprovadoEm: String? = null,
    val expiresAt: String? = null,
    val concluidoEm: String? = null,
    val material: SolicitacaoMaterialDTO? = null,
    val estudante: SolicitacaoStudentDTO? = null
)
