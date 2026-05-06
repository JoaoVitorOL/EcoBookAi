package com.ecobook.dto

data class MaterialDTO(
    val id: String,
    val titulo: String,
    val descricao: String,
    val disciplina: String,
    val nivelEnsino: String,
    val ano: Int?,
    val sistemaEnsino: String,
    val estadoConservacao: String,
    val status: String,
    val imagemUrl: String?,
    val uploadId: String?,
    val doador: MaterialDonorDTO,
    val cidade: String,
    val bairro: String,
    val dataPublicacao: Int?,
    val statusIa: String?,
    val confiancaIa: Double?,
    val criadoEm: String?,
    val atualizadoEm: String?
)
