package moe.telecom.loclogger.ui.component.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import moe.telecom.loclogger.ui.theme.LocalUiMode
import moe.telecom.loclogger.ui.theme.UiMode
import moe.telecom.loclogger.ui.theme.isInDarkTheme
import top.yukonga.miuix.kmp.blur.LocalLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlendColorEntry

/**
 * Liquid Glass 卡片容器。
 *
 * - Miuix + LayerBackdrop 可用：textureBlur 渲染毛玻璃
 * - 其他情况：退化半透明 Card
 *
 * 用法：GlassCard(modifier) { 内容 }
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val uiMode = LocalUiMode.current
    val isDark = isInDarkTheme()
    val containerColor = if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.White.copy(alpha = 0.55f)
    }

    val layerBackdrop = if (uiMode == UiMode.Miuix) LocalLayerBackdrop.current else null

    if (layerBackdrop != null) {
        val blendColor = if (isDark) {
            Color.Black.copy(alpha = 0.20f)
        } else {
            Color.White.copy(alpha = 0.45f)
        }
        Box(
            modifier = modifier
                .clip(shape)
                .textureBlur(
                    backdrop = layerBackdrop,
                    shape = shape,
                    blurRadius = 12f,
                    colors = BlurColors(
                        blendColors = listOf(BlendColorEntry(color = blendColor))
                    )
                )
        ) {
            Column(content = content)
        }
    } else {
        Box(
            modifier = modifier
                .clip(shape)
                .background(containerColor, shape)
        ) {
            Column(content = content)
        }
    }
}