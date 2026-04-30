package com.ecobook.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext context: Context
) {

    private val appContext = context.applicationContext

    private val encryptedSharedPreferences by lazy {
        createEncryptedSharedPreferences()
    }

    fun saveToken(token: String) {
        runStorageOperation { encryptedSharedPreferences.edit().putString("jwt_token", token).apply() }
    }

    fun getToken(): String? {
        return runStorageOperation { encryptedSharedPreferences.getString("jwt_token", null) }
    }

    fun clearToken() {
        runStorageOperation { encryptedSharedPreferences.edit().remove("jwt_token").apply() }
    }

    fun saveUserId(userId: String) {
        runStorageOperation { encryptedSharedPreferences.edit().putString("user_id", userId).apply() }
    }

    fun getUserId(): String? {
        return runStorageOperation { encryptedSharedPreferences.getString("user_id", null) }
    }

    fun hasToken(): Boolean {
        return !getToken().isNullOrBlank()
    }

    fun clear() {
        runStorageOperation { encryptedSharedPreferences.edit().clear().apply() }
    }

    private fun createEncryptedSharedPreferences() =
        EncryptedSharedPreferences.create(
            appContext,
            SECURE_PREFS_NAME,
            MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    private inline fun <T> runStorageOperation(operation: () -> T): T? {
        return runCatching(operation)
            .onFailure { error ->
                Timber.w(error, "Secure storage operation failed; clearing local auth state.")
                appContext.deleteSharedPreferences(SECURE_PREFS_NAME)
            }
            .getOrNull()
    }

    private companion object {
        const val SECURE_PREFS_NAME = "eco_book_secure_prefs"
    }
}
