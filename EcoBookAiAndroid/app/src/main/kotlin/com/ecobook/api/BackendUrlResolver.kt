package com.ecobook.api

import android.content.Context
import android.net.Uri
import com.ecobook.BuildConfig
import com.ecobook.R

object BackendUrlResolver {

    fun apiBaseUrl(context: Context): String {
        val runtimeOverride = RuntimeBackendUrlOverride.current(context)
        val overrideUrl = if (!runtimeOverride.isNullOrBlank()) {
            runtimeOverride
        } else {
            BuildConfig.BACKEND_URL_OVERRIDE.trim()
        }
        val configuredUrl = if (overrideUrl.isNotEmpty()) {
            overrideUrl
        } else {
            context.getString(R.string.backend_url)
        }
        return configuredUrl.trimEnd('/')
    }

    fun resolveAssetUrl(context: Context, rawUrl: String?): String? {
        val value = rawUrl?.trim().orEmpty()
        if (value.isBlank()) {
            return null
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value
        }

        val apiBase = Uri.parse(apiBaseUrl(context))
        val scheme = apiBase.scheme ?: return null
        val host = apiBase.host ?: return null
        val origin = buildString {
            append(scheme)
            append("://")
            append(host)
            apiBase.port.takeIf { it != -1 }?.let { append(":$it") }
        }
        val normalizedPath = if (value.startsWith("/")) value else "/$value"
        val basePath = apiBase.path.orEmpty().trimEnd('/')
        val resolvedPath = when {
            basePath.isBlank() -> normalizedPath
            normalizedPath == basePath || normalizedPath.startsWith("$basePath/") -> normalizedPath
            else -> basePath + normalizedPath
        }

        return origin + resolvedPath
    }
}
