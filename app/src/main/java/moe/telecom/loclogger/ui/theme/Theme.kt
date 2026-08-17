package moe.telecom.loclogger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.dynamicColorScheme
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

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
    dynamicColor: Boolean = false,
    pureBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkMode = colorMode.isDark || (colorMode.isSystem && isSystemInDarkTheme())
    val seedColor = Color(themeColor)
    val context = LocalContext.current

    val materialColorScheme = if (dynamicColor) {
        if (darkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        dynamicColorScheme(
            seedColor = seedColor,
            isDark = darkMode,
            isAmoled = pureBlack
        )
    }

    CompositionLocalProvider(
        LocalUiMode provides uiMode,
        LocalColorMode provides colorMode.value,
        LocalEnableBlur provides enableBlur,
        LocalEnableFloatingBottomBar provides enableFloatingBottomBar,
        LocalEnableFloatingBottomBarBlur provides enableFloatingBottomBarBlur,
    ) {
        when (uiMode) {
            UiMode.Material -> {
                MaterialTheme(
                    colorScheme = materialColorScheme,
                    typography = AppTypography,
                    content = content
                )
            }
            UiMode.Miuix -> {
                val schemeMode = when {
                    dynamicColor && colorMode == ColorMode.DARK -> ColorSchemeMode.MonetDark
                    dynamicColor && colorMode == ColorMode.LIGHT -> ColorSchemeMode.MonetLight
                    dynamicColor -> ColorSchemeMode.MonetSystem
                    colorMode == ColorMode.DARK -> ColorSchemeMode.Dark
                    colorMode == ColorMode.LIGHT -> ColorSchemeMode.Light
                    else -> ColorSchemeMode.System
                }
                val controller = ThemeController(
                    schemeMode,
                    keyColor = if (dynamicColor) null else seedColor,
                    isDark = darkMode,
                    paletteStyle = ThemePaletteStyle.TonalSpot,
                    colorSpec = ThemeColorSpec.Spec2025,
                )
                MiuixTheme(
                    controller = controller,
                    content = {
                        MaterialTheme(
                            colorScheme = materialColorScheme,
                            typography = AppTypography,
                            content = content
                        )
                    }
                )
            }
        }
    }
}

// 对标 SukiSU：根据颜色模式判断当前是否深色（供 FloatingBottomBar 高光/阴影取色）
@Composable
@ReadOnlyComposable
fun isInDarkTheme(): Boolean {
    return when (ColorMode.fromInt(LocalColorMode.current)) {
        ColorMode.DARK -> true
        ColorMode.LIGHT -> false
        ColorMode.SYSTEM -> isSystemInDarkTheme()
    }
}
