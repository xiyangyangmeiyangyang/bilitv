package com.openclaw.bilitv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun BiliNetImage(
    model: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val safeModel = model.trim()
    if (safeModel.isBlank()) {
        Box(modifier = modifier.background(Color(0xFF121927)))
        return
    }

    val context = LocalContext.current
    val imageRequest = remember(safeModel) {
        ImageRequest.Builder(context)
            .data(safeModel)
            .addHeader("Referer", "https://www.bilibili.com")
            .addHeader("User-Agent", "Mozilla/5.0 BiliTV/1.0")
            .crossfade(true)
            .build()
    }
    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}
