package com.ecobook.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class LogoutViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val googleAuthClient = GoogleAuthClient(context)

    fun logout() {
        viewModelScope.launch {
            googleAuthClient.clearCredentialState()
            sessionManager.clearSession("Sessao encerrada neste dispositivo.")
        }
    }
}
