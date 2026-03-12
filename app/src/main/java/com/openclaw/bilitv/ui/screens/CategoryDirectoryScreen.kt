package com.openclaw.bilitv.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.openclaw.bilitv.data.repository.BiliRepository
import com.openclaw.bilitv.ui.settings.AppUiSettings
import com.openclaw.bilitv.ui.settings.RecommendationBackgroundThemes

@Composable
fun CategoryDirectoryScreen(navController: NavController) {
    var loading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var directories by remember { mutableStateOf<List<BiliRepository.PartitionDirectory>>(emptyList()) }

    BackHandler { navigateToHome(navController) }

    LaunchedEffect(Unit) {
        loading = true
        errorText = null
        runCatching { BiliRepository.fetchAllPartitionDirectories() }
            .onSuccess { directories = it }
            .onFailure { errorText = it.message ?: "分区加载失败" }
        loading = false
    }

    val theme = RecommendationBackgroundThemes.resolve(AppUiSettings.recommendationFullscreenTheme)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(theme.main))
            .padding(horizontal = 36.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "全部分区", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "仅展示目录，按分区浏览", color = Color(0xD5F2F6FF), fontSize = 13.sp)
            }
            CategoryChip(text = "返回主页") { navigateToHome(navController) }
        }

        when {
            loading -> Text(text = "正在加载分区目录...", color = Color.White, fontSize = 14.sp)
            !errorText.isNullOrBlank() -> Text(text = errorText.orEmpty(), color = Color(0xFFFFD09B), fontSize = 13.sp)
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(directories.chunked(4)) { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowItems.forEach { item ->
                                CategoryDirectoryCard(
                                    item = item,
                                    onClick = {
                                        val encodedName = Uri.encode(item.name)
                                        navController.navigate("category/${item.rid}/$encodedName")
                                    }
                                )
                            }
                            repeat((4 - rowItems.size).coerceAtLeast(0)) {
                                Spacer(modifier = Modifier.width(0.dp).weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun navigateToHome(navController: NavController) {
    navController.navigate("home") {
        launchSingleTop = true
        popUpTo(navController.graph.startDestinationId) { inclusive = false }
        restoreState = true
    }
}

@Composable
private fun RowScope.CategoryDirectoryCard(
    item: BiliRepository.PartitionDirectory,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.04f else 1f, label = "categoryDirectoryScale")
    val border by animateColorAsState(if (focused) Color(0xFFEAF0FF) else Color(0x33FFFFFF), label = "categoryDirectoryBorder")

    Column(
        modifier = Modifier
            .weight(1f)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(if (focused) Color(0x40DDE7F9) else Color(0x24FFFFFF))
            .border(BorderStroke(if (focused) 2.dp else 1.dp, border), RoundedCornerShape(16.dp))
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = item.name,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "RID ${item.rid} · ${item.count} 热度",
            color = Color(0xDDECF3FF),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CategoryChip(text: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "categoryChipScale")
    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(999.dp))
            .background(if (focused) Color(0x5AD8E5FF) else Color(0x24FFFFFF))
            .border(
                BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFE8EEFF) else Color(0x30FFFFFF)),
                RoundedCornerShape(999.dp)
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 14.sp)
    }
}
