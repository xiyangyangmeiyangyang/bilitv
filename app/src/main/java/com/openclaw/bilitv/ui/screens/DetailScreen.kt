package com.openclaw.bilitv.ui.screens

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
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
fun DetailScreen(videoId: String, navController: NavController) {
    var video by remember(videoId) { mutableStateOf<VideoCard?>(null) }
    var related by remember(videoId) { mutableStateOf<List<VideoCard>>(emptyList()) }
    var loading by remember(videoId) { mutableStateOf(true) }
    var errorText by remember(videoId) { mutableStateOf<String?>(null) }

    LaunchedEffect(videoId) {
        loading = true
        errorText = null
        runCatching { BiliRepository.fetchVideoDetail(videoId) }
            .onSuccess {
                if (it != null) {
                    video = it
                } else {
                    errorText = "未获取到视频详情"
                }
            }
            .onFailure { errorText = it.message ?: "详情加载失败" }

        runCatching { BiliRepository.fetchRelatedVideos(bvid = videoId, limit = 5) }
            .onSuccess { if (it.isNotEmpty()) related = it }
            .onFailure { if (errorText.isNullOrBlank()) errorText = it.message ?: "相关推荐加载失败" }
        loading = false
    }

    BackHandler { navigateToHome(navController) }

    val current = video

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF07101C), Color(0xFF090B12), Color(0xFF06070B))
                )
            )
            .drawBehind {
                drawCircle(
                    color = Color(0x203D86FF),
                    radius = 420f,
                    center = Offset(size.width * 0.82f, size.height * 0.08f)
                )
            }
            .padding(horizontal = 42.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        DetailTopBar(navController)
        when {
            loading -> {
                Text(text = "正在加载详情...", color = Color(0xFFC7D4E8), fontSize = 14.sp)
            }
            current != null -> {
                val playbackQueue = buildList {
                    add(current)
                    addAll(related.filter { it.id != current.id })
                }
                DetailHero(current, playbackQueue, navController)
                DetailMeta(current)
                if (related.isNotEmpty()) {
                    RelatedSection(related, navController)
                } else {
                    Text(text = "暂无相关推荐", color = Color(0xFF98A6C3), fontSize = 13.sp)
                }
            }
            else -> {
                Text(text = errorText ?: "详情暂无数据", color = Color(0xFFFFCF9B), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun DetailTopBar(navController: NavController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "BiliTV",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold
        )

        DetailChip(text = "返回") {
            navigateToHome(navController)
        }
    }
}

@Composable
private fun DetailHero(item: VideoCard, playbackQueue: List<VideoCard>, navController: NavController) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.02f else 1f, label = "detailHeroScale")
    val border by animateColorAsState(if (focused) Color(0xFFDDE6FF) else Color(0x18FFFFFF), label = "detailHeroBorder")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .scale(scale)
            .clip(RoundedCornerShape(34.dp))
            .background(Color(0xFF121827))
            .border(BorderStroke(if (focused) 2.dp else 1.dp, border), RoundedCornerShape(34.dp))
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) {
                PlaybackQueueStore.setQueue(playbackQueue.map { it.id }, item.id)
                navController.navigate("player/${item.id}")
            }
    ) {
        BiliNetImage(
            model = item.cover,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xCC081018), Color(0x7A081018), Color(0x22081018))
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0x80060A12))
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(26.dp)
                .width(620.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x20FFFFFF))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text = item.category, color = Color.White, fontSize = 12.sp)
            }

            Text(
                text = item.title,
                color = Color.White,
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = item.description,
                color = Color(0xFFD8E1F0),
                fontSize = 15.sp,
                lineHeight = 21.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetaPill("UP主 ${item.author}")
                MetaPill(item.publishDate.ifBlank { "日期未知" })
                MetaPill(item.duration)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailActionButton(text = "立即播放", strong = true) {
                    PlaybackQueueStore.setQueue(playbackQueue.map { it.id }, item.id)
                    navController.navigate("player/${item.id}")
                }
                DetailActionButton(text = "返回首页", strong = false) {
                    navigateToHome(navController)
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
private fun DetailMeta(item: VideoCard) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetaPill(item.viewCount)
        MetaPill(item.danmakuCount)
        MetaPill(item.likeCount)
        MetaPill("分区 ${item.category}")
    }
}

@Composable
private fun RelatedSection(items: List<VideoCard>, navController: NavController) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "继续观看",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "详情页下方直接给出相关推荐，遥控器向下就能自然进入下一层内容。",
            color = Color(0xFF98A6C3),
            fontSize = 13.sp
        )
        LazyRow(
            contentPadding = PaddingValues(end = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items) { item ->
                RelatedCard(item = item) {
                    navController.navigate("detail/${item.id}")
                }
            }
        }
    }
}

@Composable
private fun RelatedCard(item: VideoCard, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.05f else 1f, label = "relatedScale")
    val border by animateColorAsState(if (focused) Color(0xFFDDE6FF) else Color(0x18FFFFFF), label = "relatedBorder")

    Column(
        modifier = Modifier
            .width(320.dp)
            .scale(scale)
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF121927))
                .border(BorderStroke(if (focused) 2.dp else 1.dp, border), RoundedCornerShape(26.dp))
        ) {
            BiliNetImage(
                model = item.cover,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Text(
            text = item.title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${item.author} · ${item.viewCount}",
            color = Color(0xFF98A6C3),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DetailChip(text: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, label = "detailChipScale")
    val bg by animateColorAsState(if (focused) Color(0x55DDE6FF) else Color(0x18FFFFFF), label = "detailChipBg")
    val border by animateColorAsState(if (focused) Color(0xFFDDE6FF) else Color(0x22FFFFFF), label = "detailChipBorder")

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(BorderStroke(if (focused) 2.dp else 1.dp, border), RoundedCornerShape(999.dp))
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(text = text, color = Color(0xFFF3F6FF), fontSize = 14.sp)
    }
}

@Composable
private fun DetailActionButton(text: String, strong: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "detailActionScale")
    val bg by animateColorAsState(
        when {
            focused && strong -> Color.White
            focused && !strong -> Color(0x44FFFFFF)
            strong -> Color(0xFFEFF4FF)
            else -> Color(0x20FFFFFF)
        },
        label = "detailActionBg"
    )
    val border by animateColorAsState(if (focused) Color(0xFFDDE6FF) else Color(0x22FFFFFF), label = "detailActionBorder")

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(BorderStroke(if (focused) 2.dp else 1.dp, if (strong && !focused) Color.Transparent else border), RoundedCornerShape(999.dp))
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp)
    ) {
        Text(
            text = text,
            color = if (strong || focused) Color(0xFF09111F) else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MetaPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0x14FFFFFF))
            .border(BorderStroke(1.dp, Color(0x18FFFFFF)), RoundedCornerShape(999.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text = text, color = Color(0xFFD5DDF0), fontSize = 13.sp)
    }
}
