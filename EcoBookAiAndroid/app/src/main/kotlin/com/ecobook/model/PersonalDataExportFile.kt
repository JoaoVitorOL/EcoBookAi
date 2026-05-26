package com.ecobook.model

import java.util.UUID

data class PersonalDataExportFile(
    val fileName: String,
    val bytes: ByteArray,
    val requestId: String = UUID.randomUUID().toString()
)
