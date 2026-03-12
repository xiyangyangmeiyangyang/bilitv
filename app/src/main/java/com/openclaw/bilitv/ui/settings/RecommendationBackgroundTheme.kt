package com.openclaw.bilitv.ui.settings

import androidx.compose.ui.graphics.Color

data class RecommendationBackgroundTheme(
    val id: Int,
    val label: String,
    val main: List<Color>,
    val glow: List<Color>
)

object RecommendationBackgroundThemes {
    val all = listOf(
        RecommendationBackgroundTheme(
            id = 0,
            label = "冰川蓝",
            main = listOf(Color(0xFF7DA0C8), Color(0xFF4F7095), Color(0xFF243B57)),
            glow = listOf(Color(0x7ABAD5F5), Color(0x330E2038))
        ),
        RecommendationBackgroundTheme(
            id = 1,
            label = "晨雾银",
            main = listOf(Color(0xFFE7ECF3), Color(0xFFC8D4E3), Color(0xFF9FB4CC)),
            glow = listOf(Color(0x6AFFFFFF), Color(0x2FA2B7D1))
        ),
        RecommendationBackgroundTheme(
            id = 2,
            label = "深海青",
            main = listOf(Color(0xFF3A5E71), Color(0xFF27404E), Color(0xFF152831)),
            glow = listOf(Color(0x6A5F9EBE), Color(0x2A091420))
        ),
        RecommendationBackgroundTheme(
            id = 3,
            label = "暖日橙",
            main = listOf(Color(0xFFB27659), Color(0xFF8A563E), Color(0xFF533124)),
            glow = listOf(Color(0x7AF2C59D), Color(0x2F2C1510))
        ),
        RecommendationBackgroundTheme(
            id = 4,
            label = "薄荷绿",
            main = listOf(Color(0xFF7BA8A3), Color(0xFF517975), Color(0xFF2C4745)),
            glow = listOf(Color(0x6ABEE8E3), Color(0x2F0F1F1E))
        ),
        RecommendationBackgroundTheme(
            id = 5,
            label = "夜幕灰",
            main = listOf(Color(0xFF4A5260), Color(0xFF343B47), Color(0xFF232A34)),
            glow = listOf(Color(0x6A9DA8B8), Color(0x2F10131A))
        )
    )

    fun resolve(themeId: Int): RecommendationBackgroundTheme {
        return all.firstOrNull { it.id == themeId } ?: all.first()
    }
}
