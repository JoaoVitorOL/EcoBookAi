package com.ecobook.fcm

import android.content.Context
import com.ecobook.api.FcmApiService
import com.ecobook.dto.FcmTokenRequestDTO
import com.ecobook.utils.SecureStorage
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
class FcmTokenSyncManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val fcmApiService: FcmApiService,
    private val secureStorage: SecureStorage
) {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun syncCachedOrCurrentTokenAsync() {
        secureStorage.getDiscoveredFcmToken()
            ?.takeIf { it.isNotBlank() }
            ?.let(::syncTokenAsync)

        if (!ensureFirebaseConfigured()) {
            Timber.i("Firebase is not configured for this build. Skipping live FCM token fetch.")
            return
        }

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Timber.w(task.exception, "Unable to fetch the current FCM token.")
                    return@addOnCompleteListener
                }

                task.result
                    ?.takeIf { it.isNotBlank() }
                    ?.let(::syncTokenAsync)
            }
    }

    fun syncTokenAsync(token: String) {
        val normalizedToken = token.trim()
        if (normalizedToken.isEmpty()) {
            return
        }

        secureStorage.saveDiscoveredFcmToken(normalizedToken)
        if (!secureStorage.hasToken()) {
            Timber.d("FCM token cached locally until an authenticated session is available.")
            return
        }

        if (secureStorage.getLastSyncedFcmToken() == normalizedToken) {
            Timber.d("FCM token already synced for the active session.")
            return
        }

        syncScope.launch {
            runCatching {
                val response = fcmApiService.registerToken(FcmTokenRequestDTO(token = normalizedToken))
                if (!response.isSuccessful) {
                    error("Backend returned HTTP ${response.code()} while syncing the FCM token.")
                }
                secureStorage.saveLastSyncedFcmToken(normalizedToken)
                Timber.d("FCM token synced with backend.")
            }.onFailure { error ->
                Timber.w(error, "Unable to sync the FCM token with the backend.")
            }
        }
    }

    private fun ensureFirebaseConfigured(): Boolean {
        return runCatching {
            FirebaseApp.getApps(appContext).isNotEmpty() || FirebaseApp.initializeApp(appContext) != null
        }.getOrElse { error ->
            Timber.w(error, "Firebase is unavailable for this build.")
            false
        }
    }
}
