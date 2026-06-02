package com.ecobook.ui

import com.ecobook.dto.UserConsentStatusDTO
import com.ecobook.model.BackendStatus
import com.ecobook.model.DonationPreview
import com.ecobook.model.NecessidadeAcademica
import com.ecobook.model.ProjectInsight
import com.ecobook.model.ReferenceDataCatalog
import com.ecobook.model.SessionUiState
import com.ecobook.model.UserProfileDraft

data class EcoBookUiState(
    val session: SessionUiState = SessionUiState(),
    val backendStatus: BackendStatus = BackendStatus.checking(),
    val profile: UserProfileDraft = UserProfileDraft(),
    val darkThemeOverride: Boolean? = null,
    val isSavingProfile: Boolean = false,
    val isUploadingProfilePhoto: Boolean = false,
    val profileFieldErrors: Map<String, String> = emptyMap(),
    val isUpdatingAiConsent: Boolean = false,
    val isLoadingConsentStatus: Boolean = false,
    val consentStatus: UserConsentStatusDTO? = null,
    val isDeletingAccount: Boolean = false,
    val accountDeletionMessage: String? = null,
    val accountDeletionMessageIsError: Boolean = false,
    val pendingAiConsent: Boolean? = null,
    val profileMessage: String? = null,
    val profileMessageIsError: Boolean = false,
    val necessidadesAcademicasDisponiveis: List<NecessidadeAcademica> = ReferenceDataCatalog.defaults().necessidadesAcademicas,
    val insights: List<ProjectInsight> = emptyList(),
    val donationPreview: DonationPreview = DonationPreview()
)
