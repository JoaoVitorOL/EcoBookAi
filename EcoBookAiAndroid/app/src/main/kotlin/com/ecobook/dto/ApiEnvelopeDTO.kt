package com.ecobook.dto

data class ApiEnvelopeDTO<T>(
    val status: Int,
    val message: String,
    val timestamp: String,
    val path: String,
    val data: T?
)
