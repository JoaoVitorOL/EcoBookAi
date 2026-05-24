package com.ecobook.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ecobook.model.DonationStep
import com.ecobook.model.MaterialHighlight
import com.ecobook.model.ProjectInsight
import com.ecobook.ui.theme.EcoBookTone
import com.ecobook.ui.theme.ecoBookBadgeColors
import com.ecobook.ui.theme.ecoBookCardShadowElevation
import com.ecobook.ui.theme.ecoBookCardTonalElevation
import com.ecobook.ui.theme.ecoBookGlassCardColor
import com.ecobook.ui.theme.ecoBookUnreadBadgeColors

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ecoBookGlassCardColor(),
        tonalElevation = ecoBookCardTonalElevation(),
        shadowElevation = ecoBookCardShadowElevation(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
fun AdaptiveScreenContent(
    modifier: Modifier = Modifier,
    maxContentWidth: Dp = 840.dp,
    content: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        content(
            Modifier
                .width(minOf(maxWidth, maxContentWidth))
                .fillMaxSize()
        )
    }
}

@Composable
fun SectionHeading(
    title: String,
    subtitle: String,
    trailingContent: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (trailingContent != null) {
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = trailingContent
            )
        }
    }
}

@Composable
private fun toneColors(tone: EcoBookTone): Pair<Color, Color> {
    val colors = ecoBookBadgeColors(tone)
    return colors.containerColor to colors.contentColor
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsEntryPointButton(
    unreadCount: Int,
    onClick: () -> Unit
) {
    val unreadBadgeColors = ecoBookUnreadBadgeColors()
    val contentDescription = if (unreadCount > 0) {
        "Abrir notificaÃ§Ãµes. $unreadCount novas notificaÃ§Ãµes."
    } else {
        "Abrir notificaÃ§Ãµes. Nenhuma novidade."
    }

    if (unreadCount > 0) {
        val badgeLabel = if (unreadCount > 99) "99+" else unreadCount.toString()

        BadgedBox(
            badge = {
                Badge(
                    containerColor = unreadBadgeColors.containerColor,
                    contentColor = unreadBadgeColors.contentColor
                ) {
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        ) {
            FilledTonalIconButton(
                onClick = onClick,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Notifications,
                    contentDescription = contentDescription
                )
            }
        }
        return
    }

    OutlinedIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.outlinedIconButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(
            imageVector = Icons.Rounded.Notifications,
            contentDescription = contentDescription
        )
    }
}

@Composable
fun StatusBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChipCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    maxLines: Int = 1,
    textAlign: TextAlign = TextAlign.Start
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(999.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                textAlign = textAlign,
                minLines = minLines,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
fun MaterialHighlightCard(material: MaterialHighlight) {
    val disciplineColors = toneColors(EcoBookTone.Success)
    val levelColors = toneColors(EcoBookTone.Warning)
    val systemColors = toneColors(EcoBookTone.Accent)

    GlassCard {
        Text(
            text = material.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = material.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusBadge(
                text = material.discipline.label,
                containerColor = disciplineColors.first,
                contentColor = disciplineColors.second
            )
            StatusBadge(
                text = material.level.label,
                containerColor = levelColors.first,
                contentColor = levelColors.second
            )
            StatusBadge(
                text = material.teachingSystem.label,
                containerColor = systemColors.first,
                contentColor = systemColors.second
            )
        }
        Text(
            text = "Ano: ${material.schoolYear} | PublicaÃ§Ã£o: ${material.publicationYear}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Local: ${material.locationLabel}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "ConservaÃ§Ã£o: ${material.conservationState.label}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = material.matchNote,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun InsightCard(insight: ProjectInsight) {
    GlassCard {
        Text(
            text = insight.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = insight.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun JourneyStepCard(
    step: DonationStep,
    index: Int
) {
    val stepColors = toneColors(EcoBookTone.Success)

    GlassCard {
        StatusBadge(
            text = "Etapa ${index + 1}",
            containerColor = stepColors.first,
            contentColor = stepColors.second
        )
        Text(
            text = step.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = step.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
