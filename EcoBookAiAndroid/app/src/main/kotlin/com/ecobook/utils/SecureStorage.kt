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

    fun saveUserName(name: String) {
        runStorageOperation { encryptedSharedPreferences.edit().putString("user_name", name).apply() }
    }

    fun getUserName(): String? {
        return runStorageOperation { encryptedSharedPreferences.getString("user_name", null) }
    }

    fun saveUserEmail(email: String) {
        runStorageOperation { encryptedSharedPreferences.edit().putString("user_email", email).apply() }
    }

    fun getUserEmail(): String? {
        return runStorageOperation { encryptedSharedPreferences.getString("user_email", null) }
    }

    fun saveUserRole(role: String) {
        runStorageOperation { encryptedSharedPreferences.edit().putString("user_role", role).apply() }
    }

    fun getUserRole(): String? {
        return runStorageOperation { encryptedSharedPreferences.getString("user_role", null) }
    }

    fun saveProfileComplete(profileComplete: Boolean) {
        runStorageOperation { encryptedSharedPreferences.edit().putBoolean("profile_complete", profileComplete).apply() }
    }

    fun getProfileComplete(): Boolean {
        return runStorageOperation { encryptedSharedPreferences.getBoolean("profile_complete", false) } ?: false
    }

    fun saveUserWhatsapp(whatsapp: String?) {
        runStorageOperation { encryptedSharedPreferences.edit().putString("user_whatsapp", whatsapp).apply() }
    }

    fun getUserWhatsapp(): String? {
        return runStorageOperation { encryptedSharedPreferences.getString("user_whatsapp", null) }
    }

    fun saveUserCidade(cidade: String?) {
        runStorageOperation { encryptedSharedPreferences.edit().putString("user_cidade", cidade).apply() }
    }

    fun getUserCidade(): String? {
        return runStorageOperation { encryptedSharedPreferences.getString("user_cidade", null) }
    }

    fun saveUserBairro(bairro: String?) {
        runStorageOperation { encryptedSharedPreferences.edit().putString("user_bairro", bairro).apply() }
    }

    fun getUserBairro(): String? {
        return runStorageOperation { encryptedSharedPreferences.getString("user_bairro", null) }
    }

    fun saveUserInstituicao(instituicao: String?) {
        runStorageOperation { encryptedSharedPreferences.edit().putString("user_instituicao", instituicao).apply() }
    }

    fun getUserInstituicao(): String? {
        return runStorageOperation { encryptedSharedPreferences.getString("user_instituicao", null) }
    }

    fun saveConsentimentoIa(consentimentoIa: Boolean) {
        runStorageOperation { encryptedSharedPreferences.edit().putBoolean("user_consentimento_ia", consentimentoIa).apply() }
    }

    fun getConsentimentoIa(): Boolean {
        return runStorageOperation { encryptedSharedPreferences.getBoolean("user_consentimento_ia", false) } ?: false
    }

    fun saveDiscoveredFcmToken(token: String) {
        runStorageOperation { encryptedSharedPreferences.edit().putString("discovered_fcm_token", token).apply() }
    }

    fun getDiscoveredFcmToken(): String? {
        return runStorageOperation { encryptedSharedPreferences.getString("discovered_fcm_token", null) }
    }

    fun saveLastSyncedFcmToken(token: String) {
        runStorageOperation { encryptedSharedPreferences.edit().putString("last_synced_fcm_token", token).apply() }
    }

    fun getLastSyncedFcmToken(): String? {
        return runStorageOperation { encryptedSharedPreferences.getString("last_synced_fcm_token", null) }
    }

    fun saveNotificationInboxPayload(payload: String) {
        runStorageOperation { encryptedSharedPreferences.edit().putString("notification_inbox_payload", payload).apply() }
    }

    fun getNotificationInboxPayload(): String? {
        return runStorageOperation { encryptedSharedPreferences.getString("notification_inbox_payload", null) }
    }

    fun hasToken(): Boolean {
        return !getToken().isNullOrBlank()
    }

    fun clearProfileData() {
        runStorageOperation {
            encryptedSharedPreferences.edit()
                .remove("user_whatsapp")
                .remove("user_cidade")
                .remove("user_bairro")
                .remove("user_instituicao")
                .remove("user_consentimento_ia")
                .remove("last_synced_fcm_token")
                .remove("notification_inbox_payload")
                .apply()
        }
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
