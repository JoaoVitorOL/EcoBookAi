package com.ecobook.dto

data class PagedResponseDTO<T>(
    val results: List<T> = emptyList(),
    val page: Int = 0,
    val size: Int = 20,
    val total: Long = 0,
    val totalPages: Int = 0,
    val hasNext: Boolean = false,
    val afterId: String? = null,
    val nextAfterId: String? = null,
    val paginationMode: String? = null
)
