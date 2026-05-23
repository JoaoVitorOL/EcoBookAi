package com.ecobook.api

import com.ecobook.dto.ApiEnvelopeDTO
import com.ecobook.dto.ReferenceDataCatalogDTO
import retrofit2.Response
import retrofit2.http.GET

interface ReferenceDataApiService {

    @GET("v1/reference-data/material-options")
    suspend fun getMaterialOptions(): Response<ApiEnvelopeDTO<ReferenceDataCatalogDTO>>
}
