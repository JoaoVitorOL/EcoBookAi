package com.ecobook.api

import android.content.Context

/**
 * Shared runtime hook used by instrumentation tests to redirect the target app
 * to a local mock backend through app-owned SharedPreferences.
 */
object RuntimeBackendUrlOverride {

    private const val PREFS_NAME = "ecobook_runtime_overrides"
    private const val BACKEND_URL_KEY = "backend_url"

    fun current(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(BACKEND_URL_KEY, null)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    fun set(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(BACKEND_URL_KEY, value.trim())
            .commit()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(BACKEND_URL_KEY)
            .commit()
    }
}
