package com.ecobook.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.ecobook.api.BackendUrlResolver
import com.ecobook.di.SecureStorageEntryPoint
import dagger.hilt.android.EntryPointAccessors

@Composable
fun ProfileAvatar(
    imageUrl: String?,
    name: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resolvedModel = BackendUrlResolver.resolveAssetUrl(context, imageUrl)
    val secureStorage = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            SecureStorageEntryPoint::class.java
        ).secureStorage()
    }
    val token = secureStorage.getToken()
    val imageRequest = remember(resolvedModel, token, context) {
        resolvedModel?.let { value ->
            ImageRequest.Builder(context)
                .data(value)
                .apply {
                    if (!token.isNullOrBlank() && value.startsWith("http")) {
                        addHeader("Authorization", "Bearer $token")
                    }
                }
                .build()
        }
    }

    if (imageRequest == null) {
        ProfileAvatarPlaceholder(name = name, modifier = modifier)
        return
    }

    SubcomposeAsyncImage(
        model = imageRequest,
        contentDescription = "Foto de perfil de $name",
        modifier = modifier.clip(CircleShape),
        contentScale = ContentScale.Crop,
        loading = { ProfileAvatarPlaceholder(name = name, modifier = Modifier.fillMaxSize()) },
        error = { ProfileAvatarPlaceholder(name = name, modifier = Modifier.fillMaxSize()) },
        success = { SubcomposeAsyncImageContent() }
    )
}

@Composable
fun ProfileAvatarPlaceholder(
    name: String,
    modifier: Modifier = Modifier
) {
    val initials = remember(name) {
        name.trim()
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)
            .take(2)
            .joinToString("") { it.take(1).uppercase() }
            .ifBlank { "?" }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
