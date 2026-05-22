package com.ecobook.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ConfidenceIndicator(
    confidence: Double?,
    modifier: Modifier = Modifier
) {
    val bucket = confidenceBucket(confidence)
    val color = when (bucket) {
        ConfidenceBucket.HIGH -> Color(0xFF205447)
        ConfidenceBucket.MEDIUM -> Color(0xFF8A4C1F)
        ConfidenceBucket.LOW -> Color(0xFF6B7280)
    }
    val icon = when (bucket) {
        ConfidenceBucket.HIGH -> Icons.Rounded.CheckCircle
        ConfidenceBucket.MEDIUM -> Icons.Rounded.WarningAmber
        ConfidenceBucket.LOW -> Icons.Rounded.HelpOutline
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color
        )
        Text(
            text = confidence?.let { "${(it * 100).toInt()}% confiança" } ?: "Manual",
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

enum class ConfidenceBucket {
    HIGH,
    MEDIUM,
    LOW
}

fun confidenceBucket(confidence: Double?): ConfidenceBucket {
    return when {
        confidence == null -> ConfidenceBucket.LOW
        confidence >= 0.75 -> ConfidenceBucket.HIGH
        confidence >= 0.50 -> ConfidenceBucket.MEDIUM
        else -> ConfidenceBucket.LOW
    }
}
