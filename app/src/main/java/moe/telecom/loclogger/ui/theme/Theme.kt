package moe.telecom.loclogger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.dynamicColorScheme
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.MiuixLightColorScheme

// CompositionLocal - 对标 SukiSU
val LocalUiMode = staticCompositionLocalOf { UiMode.Material }
val LocalColorMode = staticCompositionLocalOf { ColorMode.SYSTEM.value }
val LocalEnableBlur = staticCompositionLocalOf { true }
val LocalEnableFloatingBottomBar = staticCompositionLocalOf { true }
val LocalEnableFloatingBottomBarBlur = staticCompositionLocalOf { true }

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
                    colorScheme = if (darkMode) MiuixDarkColorScheme else MiuixLightColorScheme,
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
            UiMode.Classic -> {
                val colorScheme = if (darkMode) {
                    dynamicColorScheme(
                        seedColor = ClassicRed,
                        isDark = true
                    ).copy(
                        surface = Color(0xFF1A1A1A),
                        background = Color(0xFF000000)
                    )
                } else {
                    dynamicColorScheme(
                        seedColor = ClassicRed,
                        isDark = false
                    ).copy(
                        surface = Color(0xFFF5F5F5),
                        background = Color(0xFFFFFFFF)
                    )
                }
                androidx.compose.material3.MaterialTheme(
                    colorScheme = colorScheme,
                    typography = AppTypography,
                    content = content
                )
            }
        }
    }
}
