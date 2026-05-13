package com.ecobook.ui

import com.ecobook.model.BackendStatus
import com.ecobook.model.DonationPreview
import com.ecobook.model.ProjectInsight
import com.ecobook.model.SessionUiState
import com.ecobook.model.UserProfileDraft

data class EcoBookUiState(
    val session: SessionUiState = SessionUiState(),
    val backendStatus: BackendStatus = BackendStatus.checking(),
    val profile: UserProfileDraft = UserProfileDraft(),
    val isUpdatingAiConsent: Boolean = false,
    val pendingAiConsent: Boolean? = null,
    val profileMessage: String? = null,
    val profileMessageIsError: Boolean = false,
    val insights: List<ProjectInsight> = emptyList(),
    val donationPreview: DonationPreview = DonationPreview()
)
