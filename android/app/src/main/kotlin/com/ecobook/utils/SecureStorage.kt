package com.ecobook.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStorage(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedSharedPreferences = EncryptedSharedPreferences.create(
        context,
        "eco_book_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String) {
        encryptedSharedPreferences.edit().putString("jwt_token", token).apply()
    }

    fun getToken(): String? {
        return encryptedSharedPreferences.getString("jwt_token", null)
    }

    fun clearToken() {
        encryptedSharedPreferences.edit().remove("jwt_token").apply()
    }

    fun saveUserId(userId: String) {
        encryptedSharedPreferences.edit().putString("user_id", userId).apply()
    }

    fun getUserId(): String? {
        return encryptedSharedPreferences.getString("user_id", null)
    }

    fun clear() {
        encryptedSharedPreferences.edit().clear().apply()
    }
}
