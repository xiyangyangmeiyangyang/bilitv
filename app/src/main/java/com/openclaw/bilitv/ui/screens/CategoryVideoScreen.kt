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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
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
import com.openclaw.bilitv.ui.settings.AppUiSettings
import com.openclaw.bilitv.ui.settings.RecommendationBackgroundThemes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private object CategoryScreenFocusCache {
    val focusVideoByRid: MutableMap<Int, String> = mutableMapOf()
}

@Composable
fun CategoryVideoScreen(rid: Int, name: String, navController: NavController) {
    val displayName = remember(name, rid) {
        Uri.decode(name).ifBlank { "分区 $rid" }
    }
    var loading by remember(rid) { mutableStateOf(true) }
    var loadingMore by remember(rid) { mutableStateOf(false) }
    var hasMore by remember(rid) { mutableStateOf(true) }
    var currentPage by remember(rid) { mutableIntStateOf(1) }
    var errorText by remember(rid) { mutableStateOf<String?>(null) }
    var videos by remember(rid) { mutableStateOf<List<VideoCard>>(emptyList()) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val firstFocusRequester = remember { FocusRequester() }
    val restoreFocusRequester = remember { FocusRequester() }
    var pendingRestoreFocusVideoId by remember(rid) { mutableStateOf(CategoryScreenFocusCache.focusVideoByRid[rid]) }
    val scope = rememberCoroutineScope()

    BackHandler {
        if (!navController.popBackStack()) {
            navigateToHome(navController)
        }
    }

    LaunchedEffect(rid, refreshKey) {
        loading = true
        errorText = null
        currentPage = 1
        hasMore = true
        runCatching { BiliRepository.fetchPartitionVideosByRid(rid, page = currentPage, limit = 48) }
            .onSuccess {
                videos = it.distinctBy { card -> card.id }
                hasMore = it.size >= 48
            }
            .onFailure {
                videos = emptyList()
                errorText = it.message ?: "分区内容加载失败"
                hasMore = false
            }
        loading = false
    }

    LaunchedEffect(loading, videos.firstOrNull()?.id, videos.size) {
        if (!loading && videos.isNotEmpty()) {
            delay(40)
            val restoreId = pendingRestoreFocusVideoId
            if (!restoreId.isNullOrBlank() && videos.any { it.id == restoreId }) {
                runCatching { restoreFocusRequester.requestFocus() }
            } else {
                runCatching { firstFocusRequester.requestFocus() }
            }
            pendingRestoreFocusVideoId = null
            CategoryScreenFocusCache.focusVideoByRid.remove(rid)
        }
    }

    val theme = RecommendationBackgroundThemes.resolve(AppUiSettings.recommendationFullscreenTheme)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(theme.main))
            .padding(horizontal = 36.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = displayName, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "分区推荐视频", color = Color(0xD5F2F6FF), fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CategoryMiniChip(text = "刷新") { refreshKey += 1 }
                CategoryMiniChip(text = "返回分区目录") {
                    if (!navController.popBackStack()) {
                        navigateToHome(navController)
                    }
                }
            }
        }

        when {
            loading -> Text(text = "正在加载 $displayName ...", color = Color.White, fontSize = 14.sp)
            !errorText.isNullOrBlank() -> Text(text = errorText.orEmpty(), color = Color(0xFFFFD09B), fontSize = 13.sp)
            videos.isEmpty() -> Text(text = "$displayName 暂无可展示视频", color = Color.White, fontSize = 14.sp)
            else -> {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val spacing = 14.dp
                    val cardWidth = (maxWidth - spacing * 3) / 4
                    val rows = videos.chunked(4)
                    val totalItems = videos.size
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        itemsIndexed(rows) { rowIndex, rowItems ->
                            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                rowItems.forEachIndexed { itemIndex, item ->
                                    val globalIndex = rowIndex * 4 + itemIndex
                                    val nearEnd = hasMore && !loadingMore && totalItems > 0 && globalIndex >= (totalItems - 8).coerceAtLeast(0)
                                    CategoryVideoCard(
                                        item = item,
                                        cardWidth = cardWidth,
                                        focusRequester = when {
                                            !pendingRestoreFocusVideoId.isNullOrBlank() && pendingRestoreFocusVideoId == item.id -> restoreFocusRequester
                                            rowIndex == 0 && itemIndex == 0 -> firstFocusRequester
                                            else -> null
                                        },
                                        onFocused = if (nearEnd) {
                                            {
                                                if (!loadingMore && hasMore) {
                                                    loadingMore = true
                                                    errorText = null
                                                    val nextPage = currentPage + 1
                                                    scope.launch {
                                                        runCatching {
                                                            BiliRepository.fetchPartitionVideosByRid(rid, page = nextPage, limit = 48)
                                                        }
                                                            .onSuccess { next ->
                                                                val beforeSize = videos.size
                                                                val merged = (videos + next).distinctBy { it.id }
                                                                videos = merged
                                                                currentPage = nextPage
                                                                hasMore = next.size >= 48 && merged.size > beforeSize
                                                            }
                                                            .onFailure {
                                                                errorText = it.message ?: "加载更多失败"
                                                            }
                                                        loadingMore = false
                                                    }
                                                }
                                            }
                                        } else {
                                            null
                                        },
                                        onClick = {
                                            CategoryScreenFocusCache.focusVideoByRid[rid] = item.id
                                            PlaybackQueueStore.setQueue(videos.map { video -> video.id }, item.id)
                                            navController.navigate("player/${item.id}")
                                        }
                                    )
                                }
                                repeat((4 - rowItems.size).coerceAtLeast(0)) {
                                    Spacer(modifier = Modifier.width(cardWidth))
                                }
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                when {
                                    loadingMore -> Text(text = "正在加载更多...", color = Color(0xFFCFE0FF), fontSize = 13.sp)
                                    hasMore -> Text(text = "向下继续浏览可自动加载", color = Color(0xFFBFD3F2), fontSize = 12.sp)
                                    else -> Text(text = "没有更多内容了", color = Color(0xFFABC0DE), fontSize = 12.sp)
                                }
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
private fun CategoryVideoCard(
    item: VideoCard,
    cardWidth: androidx.compose.ui.unit.Dp,
    focusRequester: FocusRequester?,
    onFocused: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        if (focused) Color(0xFFE6EEFF) else Color(0x24FFFFFF),
        label = "categoryVideoCardBorder"
    )

    Column(
        modifier = Modifier
            .width(cardWidth)
            .let { modifier ->
                if (focusRequester != null) modifier.focusRequester(focusRequester) else modifier
            }
            .onFocusChanged { state ->
                if (state.isFocused) onFocused?.invoke()
            }
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardWidth * 0.56f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF101824))
                .border(BorderStroke(if (focused) 2.dp else 1.dp, borderColor), RoundedCornerShape(20.dp))
        ) {
            BiliNetImage(
                model = item.cover,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color(0x78000000))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${item.viewCount} · ${item.duration.ifBlank { "--:--" }}",
                    color = Color.White,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Text(
            text = item.title,
            color = Color.White,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "UP主：${item.author} · ${item.publishDate}",
            color = Color(0xFFCDDAF2),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CategoryMiniChip(text: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.05f else 1f, label = "categoryMiniChipScale")
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
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 14.sp)
    }
}
