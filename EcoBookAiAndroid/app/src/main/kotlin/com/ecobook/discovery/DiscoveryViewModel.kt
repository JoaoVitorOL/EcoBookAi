package com.ecobook.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecobook.data.ApiException
import com.ecobook.data.MaterialRepository
import com.ecobook.data.ReferenceDataRepository
import com.ecobook.data.RequestRepository
import com.ecobook.dto.MaterialDTO
import com.ecobook.model.NivelEnsino
import com.ecobook.model.ReferenceDataCatalog
import com.ecobook.utils.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val materialRepository: MaterialRepository,
    private val requestRepository: RequestRepository,
    private val referenceDataRepository: ReferenceDataRepository,
    private val secureStorage: SecureStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DiscoveryUiState(
            filters = prefilledFilters(),
            activeFilters = prefilledFilters()
        )
    )
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    init {
        loadReferenceData()
        search()
    }

    fun updateQuery(value: String) {
        updateFilters { copy(query = value) }
    }

    fun updateDisciplina(value: com.ecobook.model.Disciplina?) {
        updateFilters { copy(disciplina = value) }
    }

    fun updateNivelEnsino(value: NivelEnsino?) {
        updateFilters {
            copy(
                nivelEnsino = value,
                ano = sanitizeAnoEscolar(ano, value)
            )
        }
    }

    fun updateAno(value: String) {
        updateFilters { copy(ano = sanitizeAnoEscolar(value, nivelEnsino)) }
    }

    fun updateSistemaEnsino(value: com.ecobook.model.SistemaEnsino?) {
        updateFilters { copy(sistemaEnsino = value) }
    }

    fun updateCidade(value: String) {
        updateFilters { copy(cidade = value) }
    }

    fun updateBairro(value: String) {
        updateFilters { copy(bairro = value) }
    }

    fun updateMinAnoPublicacao(value: String) {
        updateFilters { copy(minAnoPublicacao = value.filter(Char::isDigit).take(4)) }
    }

    fun updateMaxAnoPublicacao(value: String) {
        updateFilters { copy(maxAnoPublicacao = value.filter(Char::isDigit).take(4)) }
    }

    fun search() {
        val filters = uiState.value.filters
        val validationMessage = validate(filters)
        if (validationMessage != null) {
            _uiState.update { state ->
                state.copy(toastMessage = validationMessage)
            }
            return
        }

        executeSearch(filters = filters, page = 0, append = false)
    }

    fun resetFilters() {
        val clearedFilters = DiscoveryFilters()
        _uiState.update { state ->
            state.copy(
                filters = clearedFilters,
                selectedMaterial = null,
                nextAfterId = null
            )
        }
        executeSearch(filters = clearedFilters, page = 0, append = false, afterId = null)
    }

    fun loadNextPage() {
        val state = uiState.value
        if (!state.hasSearched || !state.hasNext || state.isLoading) {
            return
        }

        executeSearch(
            filters = state.activeFilters,
            page = state.page + 1,
            append = true,
            afterId = state.nextAfterId
        )
    }

    fun openMaterialDetail(material: MaterialDTO) {
        _uiState.update { state -> state.copy(selectedMaterial = material) }
    }

    fun closeMaterialDetail() {
        _uiState.update { state -> state.copy(selectedMaterial = null) }
    }

    fun requestMaterial(materialId: String) {
        if (_uiState.value.requestingMaterialId != null) {
            return
        }

        _uiState.update { state ->
            state.copy(requestingMaterialId = materialId)
        }

        viewModelScope.launch {
            runCatching { requestRepository.createRequest(materialId) }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            selectedMaterial = null,
                            requestingMaterialId = null,
                            pendingNavigation = DiscoveryNavigation.MY_REQUESTS
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            requestingMaterialId = null,
                            toastMessage = resolveRequestError(error)
                        )
                    }
                }
        }
    }

    fun refreshActiveSearch() {
        val state = uiState.value
        if (!state.hasSearched || state.isLoading) {
            return
        }

        executeSearch(
            filters = state.activeFilters,
            page = 0,
            append = false,
            afterId = null
        )
    }

    fun consumeToast() {
        _uiState.update { state -> state.copy(toastMessage = null) }
    }

    fun consumeNavigation() {
        _uiState.update { state -> state.copy(pendingNavigation = null) }
    }

    private fun loadReferenceData() {
        viewModelScope.launch {
            val catalog = runCatching { referenceDataRepository.getCatalog() }
                .getOrElse { referenceDataRepository.defaultCatalog() }

            _uiState.update { state ->
                state.copy(
                    disciplinas = catalog.disciplinas,
                    niveisEnsino = catalog.niveisEnsino,
                    sistemasEnsino = catalog.sistemasEnsino,
                    filters = sanitizeFilters(state.filters, catalog),
                    activeFilters = sanitizeFilters(state.activeFilters, catalog)
                )
            }
        }
    }

    private fun executeSearch(
        filters: DiscoveryFilters,
        page: Int,
        append: Boolean,
        afterId: String? = null
    ) {
        if (_uiState.value.isLoading) {
            return
        }

        _uiState.update { state ->
            state.copy(
                activeFilters = filters,
                errorMessage = null,
                isLoadingInitial = !append,
                isLoadingMore = append,
                hasSearched = true,
                nextAfterId = if (append) state.nextAfterId else null
            )
        }

        viewModelScope.launch {
            runCatching {
                materialRepository.searchMaterials(
                    query = filters.query.trim().takeIf { it.isNotBlank() },
                    disciplina = filters.disciplina?.name,
                    nivelEnsino = filters.nivelEnsino?.name,
                    ano = filters.ano.toIntOrNull(),
                    sistemaEnsino = filters.sistemaEnsino?.name,
                    cidade = filters.cidade.trim().takeIf { it.isNotBlank() },
                    bairro = filters.bairro.trim().takeIf { it.isNotBlank() },
                    minAnoPublicacao = filters.minAnoPublicacao.toIntOrNull(),
                    maxAnoPublicacao = filters.maxAnoPublicacao.toIntOrNull(),
                    afterId = afterId,
                    page = page,
                    size = uiState.value.pageSize
                )
            }.onSuccess { response ->
                _uiState.update { state ->
                    state.copy(
                        results = if (append) state.results + response.results else response.results,
                        page = response.page,
                        total = response.total,
                        hasNext = response.hasNext,
                        nextAfterId = response.nextAfterId,
                        isLoadingInitial = false,
                        isLoadingMore = false
                    )
                }
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        isLoadingInitial = false,
                        isLoadingMore = false,
                        errorMessage = resolveSearchError(error)
                    )
                }
            }
        }
    }

    private fun validate(filters: DiscoveryFilters): String? {
        val ano = filters.ano.toIntOrNull()
        val minAnoPublicacao = filters.minAnoPublicacao.toIntOrNull()
        val maxAnoPublicacao = filters.maxAnoPublicacao.toIntOrNull()

        if (filters.nivelEnsino == NivelEnsino.SUPERIOR && filters.ano.isNotBlank()) {
            return "Materiais de nível superior não usam ano escolar."
        }
        if (filters.ano.isNotBlank()) {
            val maxAno = maxAnoEscolar(filters.nivelEnsino)
            if (ano == null || ano !in 1..maxAno) {
                return "Informe um ano escolar válido para o nível selecionado."
            }
        }
        if (filters.minAnoPublicacao.isNotBlank() && (minAnoPublicacao == null || minAnoPublicacao !in 1900..2100)) {
            return "O ano inicial de publicação deve ficar entre 1900 e 2100."
        }
        if (filters.maxAnoPublicacao.isNotBlank() && (maxAnoPublicacao == null || maxAnoPublicacao !in 1900..2100)) {
            return "O ano final de publicação deve ficar entre 1900 e 2100."
        }
        if (minAnoPublicacao != null && maxAnoPublicacao != null && minAnoPublicacao > maxAnoPublicacao) {
            return "O ano inicial de publicação não pode ser maior que o ano final."
        }
        return null
    }

    private fun resolveSearchError(error: Throwable): String {
        return when (error) {
            is ApiException -> when (error.statusCode) {
                400, 422 -> error.message
                401 -> "Sua sessão expirou. Entre novamente para continuar."
                403 -> "Conclua o onboarding para buscar materiais disponíveis."
                else -> "Não foi possível carregar os materiais agora."
            }

            is SocketTimeoutException -> "A busca demorou demais para responder."
            is ConnectException,
            is UnknownHostException,
            is IOException -> "Não foi possível conectar ao backend configurado no app."
            else -> error.message ?: "Falha inesperada ao buscar materiais."
        }
    }

    private fun resolveRequestError(error: Throwable): String {
        return when (error) {
            is ApiException -> when (error.statusCode) {
                400 -> error.message
                401 -> "Sua sessão expirou. Entre novamente para continuar."
                403 -> "Conclua o onboarding para solicitar materiais."
                404 -> "Esse material não foi encontrado."
                409 -> error.message
                422 -> "Esse material não está mais disponível para novas solicitações."
                else -> error.message
            }

            is ConnectException,
            is UnknownHostException,
            is IOException -> "Não foi possível enviar a solicitação porque o backend não respondeu."
            else -> error.message ?: "Falha inesperada ao enviar a solicitação."
        }
    }

    private fun updateFilters(transform: DiscoveryFilters.() -> DiscoveryFilters) {
        _uiState.update { state ->
            state.copy(filters = state.filters.transform())
        }
    }

    private fun prefilledFilters(): DiscoveryFilters {
        return DiscoveryFilters(
            cidade = secureStorage.getUserCidade().orEmpty(),
            bairro = secureStorage.getUserBairro().orEmpty()
        )
    }

    private fun sanitizeFilters(filters: DiscoveryFilters, catalog: ReferenceDataCatalog): DiscoveryFilters {
        val nivelEnsino = filters.nivelEnsino?.takeIf(catalog.niveisEnsino::contains)
        return filters.copy(
            disciplina = filters.disciplina?.takeIf(catalog.disciplinas::contains),
            nivelEnsino = nivelEnsino,
            ano = sanitizeAnoEscolar(filters.ano, nivelEnsino),
            sistemaEnsino = filters.sistemaEnsino?.takeIf(catalog.sistemasEnsino::contains)
        )
    }

    private fun sanitizeAnoEscolar(value: String, nivelEnsino: NivelEnsino?): String {
        if (nivelEnsino == NivelEnsino.SUPERIOR) {
            return ""
        }

        val digits = value.filter(Char::isDigit).take(1)
        val parsedYear = digits.toIntOrNull() ?: return digits
        val maxAno = maxAnoEscolar(nivelEnsino)
        return if (parsedYear in 1..maxAno) parsedYear.toString() else ""
    }

    private fun maxAnoEscolar(nivelEnsino: NivelEnsino?): Int {
        return when (nivelEnsino) {
            NivelEnsino.MEDIO -> 3
            else -> 9
        }
    }
}
