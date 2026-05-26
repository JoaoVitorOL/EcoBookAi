package com.ecobook.auth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LogoutViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    fun logout() {
        sessionManager.clearSession("Sessão encerrada neste dispositivo.")
    }
}
