package moe.telecom.loclogger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.dynamicColorScheme
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

// CompositionLocal - 对标 SukiSU
val LocalUiMode = staticCompositionLocalOf { UiMode.Material }
val LocalColorMode = staticCompositionLocalOf { ColorMode.SYSTEM.value }
val LocalEnableBlur = staticCompositionLocalOf { true }
val LocalEnableFloatingBottomBar = staticCompositionLocalOf { true }
val LocalEnableFloatingBottomBarBlur = staticCompositionLocalOf { true }
val LocalLayerBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

@Composable
fun GpsLoggerTheme(
    themeColor: Int,
    uiMode: UiMode,
    colorMode: ColorMode,
    enableBlur: Boolean = true,
    enableFloatingBottomBar: Boolean = true,
    enableFloatingBottomBarBlur: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkMode = colorMode.isDark || (colorMode.isSystem && isSystemInDarkTheme())
    val seedColor = Color(themeColor)

    CompositionLocalProvider(
        LocalUiMode provides uiMode,
        LocalColorMode provides colorMode.value,
        LocalEnableBlur provides enableBlur,
        LocalEnableFloatingBottomBar provides enableFloatingBottomBar,
        LocalEnableFloatingBottomBarBlur provides enableFloatingBottomBarBlur,
    ) {
        when (uiMode) {
            UiMode.Material -> {
                val colorScheme = dynamicColorScheme(
                    seedColor = seedColor,
                    isDark = darkMode,
                    isAmoled = false
                )
                androidx.compose.material3.MaterialTheme(
                    colorScheme = colorScheme,
                    typography = AppTypography,
                    content = content
                )
            }
            UiMode.Miuix -> {
                MiuixTheme(
                    colors = if (darkMode) darkColorScheme() else lightColorScheme(),
                    content = {
                        androidx.compose.material3.MaterialTheme(
                            colorScheme = dynamicColorScheme(
                                seedColor = seedColor,
                                isDark = darkMode
                            ),
                            typography = AppTypography,
                            content = content
                        )
                    }
                )
            }
        }
    }
}
