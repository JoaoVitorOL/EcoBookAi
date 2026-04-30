package com.ecobook.ui

import com.ecobook.model.BackendStatus
import com.ecobook.model.Disciplina
import com.ecobook.model.DonationPreview
import com.ecobook.model.MaterialHighlight
import com.ecobook.model.NivelEnsino
import com.ecobook.model.ProjectInsight
import com.ecobook.model.UserProfileDraft

data class EcoBookUiState(
    val backendStatus: BackendStatus = BackendStatus.checking(),
    val profile: UserProfileDraft = UserProfileDraft(),
    val catalog: List<MaterialHighlight> = emptyList(),
    val insights: List<ProjectInsight> = emptyList(),
    val donationPreview: DonationPreview = DonationPreview(),
    val searchQuery: String = "",
    val selectedDisciplina: Disciplina? = null,
    val selectedNivelEnsino: NivelEnsino? = null
) {
    val filteredMaterials: List<MaterialHighlight>
        get() {
            val normalizedQuery = searchQuery.trim().lowercase()

            return catalog.filter { material ->
                val matchesQuery = normalizedQuery.isBlank() ||
                    material.title.lowercase().contains(normalizedQuery) ||
                    material.summary.lowercase().contains(normalizedQuery) ||
                    material.locationLabel.lowercase().contains(normalizedQuery)

                val matchesDiscipline = selectedDisciplina == null || material.discipline == selectedDisciplina
                val matchesLevel = selectedNivelEnsino == null || material.level == selectedNivelEnsino

                matchesQuery && matchesDiscipline && matchesLevel
            }
        }
}
