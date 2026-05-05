package com.ecobook.data

class ApiException(
    val statusCode: Int,
    override val message: String,
    val fieldErrors: Map<String, String> = emptyMap()
) : RuntimeException(message)
