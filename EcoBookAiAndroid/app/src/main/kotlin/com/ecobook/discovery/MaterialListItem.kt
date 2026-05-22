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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.ecobook.api.BackendUrlResolver
import com.ecobook.dto.MaterialDTO
import com.ecobook.ui.components.GlassCard
import com.ecobook.ui.components.StatusBadge

@Composable
fun MaterialListItem(
    material: MaterialDTO,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.clickable(onClick = onClick)
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
                        containerColor = Color(0xFFE5F0EA),
                        contentColor = Color(0xFF205447)
                    )
                    StatusBadge(
                        text = formatNivelEnsino(material.nivelEnsino),
                        containerColor = Color(0xFFFCE7D8),
                        contentColor = Color(0xFF8A4C1F)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(
                        text = formatAnoEscolar(material.ano),
                        containerColor = Color(0xFFF1EAF8),
                        contentColor = Color(0xFF5E427E)
                    )
                    StatusBadge(
                        text = formatEstadoConservacao(material.estadoConservacao),
                        containerColor = Color(0xFFE6EEF8),
                        contentColor = Color(0xFF214A73)
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

    if (resolvedModel == null) {
        MaterialImagePlaceholder(modifier = modifier)
        return
    }

    SubcomposeAsyncImage(
        model = resolvedModel,
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFE3E7E3)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Image,
            contentDescription = null,
            tint = Color(0xFF738579)
        )
    }
}

private fun buildMetaLine(material: MaterialDTO): String {
    val publication = material.dataPublicacao?.let { "Publicação $it" } ?: "Publicação não informada"
    val system = formatSistemaEnsino(material.sistemaEnsino)
    val created = formatRelativeDate(material.criadoEm)?.let { "Cadastro $it" } ?: "Cadastro recente"
    return "$system | $publication | $created"
}
