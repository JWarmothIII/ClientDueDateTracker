@file:Suppress("ktlint:standard:function-naming")

package dev.jwarmothiii.clientduedatetracker.shared.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme =
    lightColorScheme(
        primary = DeepRoseGold,
        onPrimary = White,
        primaryContainer = SoftBlush,
        onPrimaryContainer = WarmCharcoal,
        secondary = RoseGold,
        onSecondary = DeepWarmCharcoal,
        secondaryContainer = SoftBlush,
        onSecondaryContainer = WarmCharcoal,
        tertiary = WarmGray,
        onTertiary = White,
        background = White,
        onBackground = WarmCharcoal,
        surface = WarmWhite,
        onSurface = WarmCharcoal,
        surfaceVariant = SoftBlush,
        onSurfaceVariant = WarmGray,
        outline = BlushGray,
        error = ErrorRed,
        onError = White,
    )

@Composable
fun ClientDueDateTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}
