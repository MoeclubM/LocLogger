package moe.telecom.loclogger.ui.theme

// 主题模式 - 对标 SukiSU UiMode.kt
enum class UiMode(val displayName: String) {
    Material("Material 3"),
    Miuix("MIUI X");

    companion object {
        fun fromInt(value: Int) = entries.firstOrNull { it.ordinal == value } ?: Material
    }
}

// 颜色模式
enum class ColorMode(val value: Int, val displayName: String) {
    SYSTEM(0, "跟随系统"),
    LIGHT(1, "浅色"),
    DARK(2, "深色");

    val isDark get() = this == DARK
    val isSystem get() = this == SYSTEM

    companion object {
        fun fromInt(value: Int) = entries.firstOrNull { it.value == value } ?: SYSTEM
    }
}
