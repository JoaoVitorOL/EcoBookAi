package com.ecobook.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ecobook.model.BackendConnectionState

@Immutable
data class EcoBookBadgeColors(
    val containerColor: Color,
    val contentColor: Color
)

enum class EcoBookTone {
    Success,
    Warning,
    Accent,
    Neutral,
    Danger
}

@Composable
fun ecoBookBadgeColors(tone: EcoBookTone): EcoBookBadgeColors {
    val colorScheme = MaterialTheme.colorScheme
    return when (tone) {
        EcoBookTone.Success -> EcoBookBadgeColors(
            containerColor = colorScheme.primaryContainer,
            contentColor = colorScheme.onPrimaryContainer
        )

        EcoBookTone.Warning -> EcoBookBadgeColors(
            containerColor = colorScheme.secondaryContainer,
            contentColor = colorScheme.onSecondaryContainer
        )

        EcoBookTone.Accent -> EcoBookBadgeColors(
            containerColor = colorScheme.tertiaryContainer,
            contentColor = colorScheme.onTertiaryContainer
        )

        EcoBookTone.Neutral -> EcoBookBadgeColors(
            containerColor = colorScheme.surfaceVariant,
            contentColor = colorScheme.onSurfaceVariant
        )

        EcoBookTone.Danger -> EcoBookBadgeColors(
            containerColor = colorScheme.errorContainer,
            contentColor = colorScheme.onErrorContainer
        )
    }
}

@Composable
fun ecoBookAppBackgroundBrush(): Brush {
    val colorScheme = MaterialTheme.colorScheme
    return if (isEcoBookDarkTheme()) {
        Brush.verticalGradient(
            colors = listOf(
                colorScheme.background,
                colorScheme.surfaceVariant.copy(alpha = 0.24f),
                colorScheme.primaryContainer.copy(alpha = 0.16f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                colorScheme.secondaryContainer.copy(alpha = 0.42f),
                colorScheme.background,
                colorScheme.tertiaryContainer.copy(alpha = 0.18f)
            )
        )
    }
}

@Composable
fun ecoBookHeroBrush(): Brush {
    val colorScheme = MaterialTheme.colorScheme
    return if (isEcoBookDarkTheme()) {
        Brush.verticalGradient(
            colors = listOf(colorScheme.primaryContainer, colorScheme.surfaceVariant)
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(colorScheme.primary, colorScheme.tertiary)
        )
    }
}

@Composable
fun ecoBookHeroContentColor(): Color {
    val colorScheme = MaterialTheme.colorScheme
    return if (isEcoBookDarkTheme()) {
        colorScheme.onPrimaryContainer
    } else {
        colorScheme.onPrimary
    }
}

@Composable
fun ecoBookGlassCardColor(): Color {
    return MaterialTheme.colorScheme.surface
}

@Composable
fun ecoBookCardTonalElevation(): Dp {
    return if (isEcoBookDarkTheme()) 6.dp else 2.dp
}

@Composable
fun ecoBookCardShadowElevation(): Dp {
    return if (isEcoBookDarkTheme()) 0.dp else 6.dp
}

@Composable
fun ecoBookNavigationBarContainerColor(): Color {
    return MaterialTheme.colorScheme.surface
}

@Composable
fun ecoBookNavigationBarTonalElevation(): Dp {
    return if (isEcoBookDarkTheme()) 3.dp else 8.dp
}

@Composable
fun ecoBookNavigationBarShadowElevation(): Dp {
    return if (isEcoBookDarkTheme()) 0.dp else 8.dp
}

@Composable
fun ecoBookUnreadBadgeColors(): EcoBookBadgeColors {
    val colorScheme = MaterialTheme.colorScheme
    return EcoBookBadgeColors(
        containerColor = colorScheme.error,
        contentColor = colorScheme.onError
    )
}

@Composable
fun ecoBookImagePlaceholderColors(): EcoBookBadgeColors {
    val colorScheme = MaterialTheme.colorScheme
    return EcoBookBadgeColors(
        containerColor = colorScheme.surfaceVariant,
        contentColor = colorScheme.onSurfaceVariant
    )
}

@Composable
fun backendConnectionBadgeColors(state: BackendConnectionState): EcoBookBadgeColors {
    return when (state) {
        BackendConnectionState.ONLINE -> ecoBookBadgeColors(EcoBookTone.Success)
        BackendConnectionState.CHECKING -> ecoBookBadgeColors(EcoBookTone.Warning)
        BackendConnectionState.OFFLINE -> ecoBookBadgeColors(EcoBookTone.Danger)
    }
}
