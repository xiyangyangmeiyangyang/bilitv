package com.openclaw.bilitv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val BiliTvColors = darkColorScheme(
    primary = Color(0xFFF2F5FF),
    onPrimary = Color(0xFF0A0D16),
    secondary = Color(0xFFA7B4FF),
    onSecondary = Color.White,
    background = Color(0xFF070B14),
    surface = Color(0xFF101826),
    onSurface = Color(0xFFF4F7FF),
)

@Composable
fun BiliTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BiliTvColors,
        content = content,
    )
}
