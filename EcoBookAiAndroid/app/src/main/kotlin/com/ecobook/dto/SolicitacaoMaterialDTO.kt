package com.ecobook.dto

data class SolicitacaoMaterialDTO(
    val id: String,
    val titulo: String,
    val descricao: String? = null,
    val imagemUrl: String? = null,
    val disciplina: String,
    val nivelEnsino: String,
    val ano: Int? = null,
    val status: String,
    val cidade: String,
    val bairro: String,
    val doadorNome: String,
    val doadorFotoPerfilUrl: String? = null
)
