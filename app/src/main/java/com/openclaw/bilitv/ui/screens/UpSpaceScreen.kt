package com.openclaw.bilitv.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.openclaw.bilitv.data.repository.BiliRepository
import com.openclaw.bilitv.model.VideoCard
import com.openclaw.bilitv.ui.components.BiliNetImage
import com.openclaw.bilitv.ui.player.PlaybackQueueStore

@Composable
fun UpSpaceScreen(videoId: String, navController: NavController) {
    var upName by remember { mutableStateOf("UP主") }
    var upAvatar by remember { mutableStateOf("") }
    var videos by remember { mutableStateOf<List<VideoCard>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(videoId) {
        runCatching { BiliRepository.fetchVideoDetail(videoId) }
            .onSuccess { detail ->
                if (detail != null) {
                    upName = detail.author
                    upAvatar = detail.avatar
                    runCatching { BiliRepository.searchVideos(detail.author) }
                        .onSuccess { list ->
                            val resolved = list
                                .filter { it.id != detail.id }
                                .distinctBy { it.id }
                                .take(20)
                            if (resolved.isNotEmpty()) videos = resolved
                        }
                        .onFailure { error = it.message ?: "UP主页加载失败" }
                }
            }
            .onFailure { error = it.message ?: "UP主页加载失败" }
    }

    BackHandler { navigateToHome(navController) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A1018), Color(0xFF0B0F15), Color(0xFF080B11))
                )
            )
            .padding(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0x1FFFFFFF))
                        .border(BorderStroke(1.dp, Color(0x30FFFFFF)), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (upAvatar.isNotBlank()) {
                        BiliNetImage(
                            model = upAvatar,
                            contentDescription = upName,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(28.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(text = "UP", color = Color.White, fontSize = 12.sp)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = upName, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "UP主页", color = Color(0xFFC0CFE6), fontSize = 13.sp)
                }
            }
            UpBackButton { navigateToHome(navController) }
        }

        error?.let {
            Text(text = it, color = Color(0xFFFFCF9B), fontSize = 12.sp)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (videos.isEmpty()) {
                item(key = "empty_hint") {
                    Text(text = "暂无可展示视频", color = Color(0xFF9CADC9), fontSize = 13.sp)
                }
            } else {
                items(videos, key = { it.id }) { item ->
                    UpVideoItem(item = item) {
                        PlaybackQueueStore.setQueue(videos.map { video -> video.id }, item.id)
                        navController.navigate("player/${item.id}")
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
private fun UpVideoItem(item: VideoCard, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(if (focused) 1.02f else 1f, label = "upItemScale")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(if (focused) Color(0x312F3B50) else Color(0x1A1A2536))
            .border(
                BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFDDE8FF) else Color(0x23FFFFFF)),
                RoundedCornerShape(16.dp)
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(90.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF101A2A))
        ) {
            BiliNetImage(
                model = item.cover,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.description,
                color = Color(0xFFC7D4E8),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun UpBackButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (focused) Color(0x5AD3E3FF) else Color(0x23141F30))
            .border(
                BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFE6EDFF) else Color(0x2BFFFFFF)),
                RoundedCornerShape(999.dp)
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(text = "返回", color = Color.White, fontSize = 14.sp)
    }
}
