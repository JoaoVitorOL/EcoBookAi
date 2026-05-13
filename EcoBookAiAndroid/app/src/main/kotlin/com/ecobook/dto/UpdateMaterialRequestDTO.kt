package com.ecobook.dto

data class UpdateMaterialRequestDTO(
    val titulo: String,
    val autor: String?,
    val editora: String?,
    val descricao: String,
    val disciplina: String,
    val nivelEnsino: String,
    val ano: Int?,
    val sistemaEnsino: String,
    val estadoConservacao: String,
    val dataPublicacao: Int?
)
