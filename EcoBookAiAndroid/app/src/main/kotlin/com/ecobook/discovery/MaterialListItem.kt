package com.ecobook.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.ecobook.api.BackendUrlResolver
import com.ecobook.di.SecureStorageEntryPoint
import com.ecobook.dto.MaterialDTO
import com.ecobook.ui.components.GlassCard
import com.ecobook.ui.components.StatusBadge
import com.ecobook.ui.theme.EcoBookTone
import com.ecobook.ui.theme.ecoBookBadgeColors
import com.ecobook.ui.theme.ecoBookImagePlaceholderColors
import dagger.hilt.android.EntryPointAccessors

@Composable
fun MaterialListItem(
    material: MaterialDTO,
    onClick: () -> Unit
) {
    val disciplineColors = ecoBookBadgeColors(EcoBookTone.Success)
    val levelColors = ecoBookBadgeColors(EcoBookTone.Warning)
    val yearColors = ecoBookBadgeColors(EcoBookTone.Accent)
    val conditionColors = ecoBookBadgeColors(EcoBookTone.Neutral)

    GlassCard(
        modifier = Modifier.clickable(
            role = Role.Button,
            onClickLabel = "Abrir detalhes do material ${material.titulo}",
            onClick = onClick
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MaterialImage(
                imageUrl = material.imagemUrl,
                title = material.titulo,
                modifier = Modifier
                    .width(96.dp)
                    .height(132.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = material.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = material.descricao,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(
                        text = formatDisciplina(material.disciplina),
                        containerColor = disciplineColors.containerColor,
                        contentColor = disciplineColors.contentColor
                    )
                    StatusBadge(
                        text = formatNivelEnsino(material.nivelEnsino),
                        containerColor = levelColors.containerColor,
                        contentColor = levelColors.contentColor
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(
                        text = formatAnoEscolar(material.ano),
                        containerColor = yearColors.containerColor,
                        contentColor = yearColors.contentColor
                    )
                    StatusBadge(
                        text = formatEstadoConservacao(material.estadoConservacao),
                        containerColor = conditionColors.containerColor,
                        contentColor = conditionColors.contentColor
                    )
                }
                Text(
                    text = "${material.bairro}, ${material.cidade}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Doador: ${material.doador.nome}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = buildMetaLine(material),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun MaterialImage(
    imageUrl: String?,
    title: String,
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
        MaterialImagePlaceholder(modifier = modifier)
        return
    }

    SubcomposeAsyncImage(
        model = imageRequest,
        contentDescription = "Capa de $title",
        modifier = modifier.clip(RoundedCornerShape(20.dp)),
        contentScale = ContentScale.Crop,
        loading = { MaterialImagePlaceholder(modifier = Modifier.fillMaxSize()) },
        error = { MaterialImagePlaceholder(modifier = Modifier.fillMaxSize()) },
        success = { SubcomposeAsyncImageContent() }
    )
}

@Composable
internal fun MaterialImagePlaceholder(
    modifier: Modifier = Modifier
) {
    val placeholderColors = ecoBookImagePlaceholderColors()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(placeholderColors.containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Image,
            contentDescription = null,
            tint = placeholderColors.contentColor
        )
    }
}

private fun buildMetaLine(material: MaterialDTO): String {
    val publication = material.dataPublicacao?.let { "Publicação $it" } ?: "Publicação não informada"
    val system = formatSistemaEnsino(material.sistemaEnsino)
    val created = formatRelativeDate(material.criadoEm)?.let { "Cadastro $it" } ?: "Cadastro recente"
    return "$system | $publication | $created"
}
