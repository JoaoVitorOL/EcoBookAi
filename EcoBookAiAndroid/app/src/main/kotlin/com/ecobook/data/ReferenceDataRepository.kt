package com.ecobook.data

import com.ecobook.api.ReferenceDataApiService
import com.ecobook.dto.ApiEnvelopeDTO
import com.ecobook.dto.ApiErrorResponseDTO
import com.ecobook.dto.ReferenceDataCatalogDTO
import com.ecobook.dto.ReferenceOptionDTO
import com.ecobook.model.Disciplina
import com.ecobook.model.EstadoConservacao
import com.ecobook.model.NecessidadeAcademica
import com.ecobook.model.NivelEnsino
import com.ecobook.model.ReferenceDataCatalog
import com.ecobook.model.SistemaEnsino
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response

@Singleton
class ReferenceDataRepository @Inject constructor(
    private val referenceDataApiService: ReferenceDataApiService,
    private val gson: Gson
) {

    private val cacheMutex = Mutex()

    @Volatile
    private var cachedCatalog: ReferenceDataCatalog? = null

    fun defaultCatalog(): ReferenceDataCatalog = ReferenceDataCatalog.defaults()

    suspend fun getCatalog(forceRefresh: Boolean = false): ReferenceDataCatalog {
        if (!forceRefresh) {
            cachedCatalog?.let { return it }
        }

        return cacheMutex.withLock {
            if (!forceRefresh) {
                cachedCatalog?.let { return@withLock it }
            }

            val fallbackCatalog = cachedCatalog ?: defaultCatalog()
            val nextCatalog = runCatching { fetchCatalog() }
                .getOrElse { fallbackCatalog }
            cachedCatalog = nextCatalog
            nextCatalog
        }
    }

    private suspend fun fetchCatalog(): ReferenceDataCatalog {
        val payload = requireData(referenceDataApiService.getMaterialOptions())
        return ReferenceDataCatalog(
            disciplinas = payload.disciplinas.mapOptions(Disciplina.entries.toList()),
            niveisEnsino = payload.niveisEnsino.mapOptions(NivelEnsino.entries.toList()),
            sistemasEnsino = payload.sistemasEnsino.mapOptions(SistemaEnsino.entries.toList()),
            estadosConservacao = payload.estadosConservacao.mapOptions(EstadoConservacao.entries.toList()),
            necessidadesAcademicas = payload.necessidadesAcademicas.mapOptions(NecessidadeAcademica.entries.toList())
        )
    }

    private fun <T> List<ReferenceOptionDTO>.mapOptions(defaults: List<T>): List<T> where T : Enum<T> {
        val mapped = mapNotNull { option ->
            defaults.firstOrNull { enumValue -> enumValue.name == option.value }
        }.distinct()
        return mapped.ifEmpty { defaults }
    }

    private fun <T> requireData(response: Response<ApiEnvelopeDTO<T>>): T {
        if (response.isSuccessful) {
            val envelope = response.body() ?: throw ApiException(response.code(), "Resposta vazia do servidor")
            return envelope.data ?: throw ApiException(response.code(), envelope.message)
        }

        val error = response.errorBody()?.string()?.takeIf { it.isNotBlank() }?.let { payload ->
            runCatching { gson.fromJson(payload, ApiErrorResponseDTO::class.java) }.getOrNull()
        }

        throw ApiException(
            statusCode = response.code(),
            message = error?.message ?: "Falha ao carregar o catalogo de referencia",
            fieldErrors = error?.fieldErrors ?: emptyMap()
        )
    }
}
