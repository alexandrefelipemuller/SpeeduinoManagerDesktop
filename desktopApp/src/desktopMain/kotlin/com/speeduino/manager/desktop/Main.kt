package com.speeduino.manager.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.speeduino.manager.desktop.app.DesktopAppShell

private val SpeeduinoColorScheme = lightColorScheme(
    primary = Color(0xFF305C4F),
    onPrimary = Color(0xFFF8F6F2),
    secondary = Color(0xFFC37B2C),
    onSecondary = Color(0xFF2A1A05),
    background = Color(0xFFF5F1E8),
    onBackground = Color(0xFF1C1B1A),
    surface = Color(0xFFFFFBF5),
    onSurface = Color(0xFF1C1B1A),
    surfaceVariant = Color(0xFFF0E7D8),
    onSurfaceVariant = Color(0xFF3B342C),
    outline = Color(0xFFB8AFA2)
)

private val SpeeduinoTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 28.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        letterSpacing = 0.6.sp
    )
)

@Composable
private fun SpeeduinoDesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SpeeduinoColorScheme,
        typography = SpeeduinoTypography,
        content = content
    )
}

fun main() = application {
    val language by LocalizationManager.language.collectAsState()
    val strings = remember(language) { Strings(Translations.forLanguage(language)) }

    Window(
        onCloseRequest = { kotlin.system.exitProcess(0) },
        title = strings["app.windowTitle"],
        state = rememberWindowState(width = 1400.dp, height = 900.dp)
    ) {
        CompositionLocalProvider(LocalStrings provides strings) {
            SpeeduinoDesktopTheme {
                DesktopAppShell()
            }
        }
    }
}
