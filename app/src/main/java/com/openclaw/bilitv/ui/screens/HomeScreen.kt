package com.openclaw.bilitv.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

private data class HomeTab(
    val label: String,
    val symbol: String,
    val opensSearch: Boolean = false
)

private data class HotTab(
    val label: String,
    val symbol: String
)

private data class FeatureIntro(
    val title: String,
    val description: String
)

private val FeatureIntros = listOf(
    FeatureIntro(
        title = "主菜单概览",
        description = "简洁直观的主菜单界面，快速访问您的媒体库、直播、动态及设置。"
    ),
    FeatureIntro(
        title = "多维融合播放",
        description = "彻底打破常规。MVision 独有的融合播放模式，支持 4K/HDR/杜比视界智能清晰度切换。让您在沉浸观影的同时，同屏掌握热力图、播放记录与实时评论。通过智能数据可视化，感知每一个精彩瞬间。"
    ),
    FeatureIntro(
        title = "媒体索引首页",
        description = "全新的视觉导视系统，为您呈现实时更新的媒体资讯与个性化推荐。"
    ),
    FeatureIntro(
        title = "直播中心",
        description = "集成丰富的直播频道，支持流畅的清晰度切换与实时预览，尽享电视直播乐趣。"
    ),
    FeatureIntro(
        title = "灵动动态与聚合",
        description = "实时捕捉您常看 UP 主的最新动态，确保每一份志趣相投的更新都不被错过。"
    ),
    FeatureIntro(
        title = "智能全局搜索",
        description = "强大的全局搜索功能，一键直达您想看的任何内容，支持多维过滤。"
    )
)

private val HomeTabs = listOf(
    HomeTab("推荐", "◎"),
    HomeTab("热门", "◌"),
    HomeTab("收藏", "❤"),
    HomeTab("搜索", "⌕", opensSearch = true),
    HomeTab("全部分区", "▦"),
    HomeTab("历史记录", "◔"),
    HomeTab("我的", "◍")
)

private val HotTabs = listOf(
    HotTab("综合热门", "热"),
    HotTab("每周必看", "周"),
    HotTab("排行榜", "榜"),
    HotTab("全站音乐榜", "音")
)

private object HomeScreenStateCache {
    var selectedTabIndex: Int = 0
    var selectedHotTabIndex: Int = 0
    var selectedHeroIndex: Int = 0
    var lastFocusedVideoId: String? = null
    val tabCache: MutableMap<Int, List<VideoCard>> = mutableMapOf()
}

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val tabCache = remember {
        mutableStateMapOf<Int, List<VideoCard>>().apply { putAll(HomeScreenStateCache.tabCache) }
    }
    val scope = rememberCoroutineScope()
    var selectedTabIndex by remember { mutableIntStateOf(HomeScreenStateCache.selectedTabIndex.coerceIn(0, HomeTabs.lastIndex)) }
    var selectedHotTabIndex by remember { mutableIntStateOf(HomeScreenStateCache.selectedHotTabIndex.coerceIn(0, HotTabs.lastIndex)) }
    var selectedIndex by remember { mutableIntStateOf(HomeScreenStateCache.selectedHeroIndex) }
    var tabVideos by remember { mutableStateOf<List<VideoCard>>(emptyList()) }
    var menuVisible by remember { mutableStateOf(false) }
    var detailVideo by remember { mutableStateOf<VideoCard?>(null) }
    var loadingTab by remember { mutableStateOf(false) }
    var tabError by remember { mutableStateOf<String?>(null) }
    var refreshRequestKey by remember { mutableIntStateOf(0) }
    var refreshingRecommendations by remember { mutableStateOf(false) }
    var authLoggedIn by remember { mutableStateOf(BiliRepository.isLoggedIn()) }
    var qrDialogVisible by remember { mutableStateOf(false) }
    var qrSession by remember { mutableStateOf<BiliRepository.QrLoginSession?>(null) }
    var qrStatus by remember { mutableStateOf<String?>(null) }
    var detailActionHint by remember { mutableStateOf<String?>(null) }
    var detailFollowed by remember { mutableStateOf<Boolean?>(null) }
    var detailFollowLoading by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var carouselControlMode by remember { mutableStateOf(false) }
    var tabLoadingMore by remember { mutableStateOf(false) }
    val pageByCacheKey = remember { mutableStateMapOf<Int, Int>() }
    val endByCacheKey = remember { mutableStateMapOf<Int, Boolean>() }
    var myProfile by remember { mutableStateOf<BiliRepository.UserProfile?>(null) }
    var myProfileLoading by remember { mutableStateOf(false) }
    var myProfileError by remember { mutableStateOf<String?>(null) }
    var pendingRestoreFocusVideoId by remember { mutableStateOf(HomeScreenStateCache.lastFocusedVideoId) }
    val menuButtonFocusRequester = remember { FocusRequester() }
    val heroPlayFocusRequester = remember { FocusRequester() }
    val heroInfoFocusRequester = remember { FocusRequester() }
    val firstGridCardFocusRequester = remember { FocusRequester() }

    val selectedTab = HomeTabs[selectedTabIndex]
    val selectedHotTab = HotTabs[selectedHotTabIndex.coerceIn(0, HotTabs.lastIndex)]
    val isRecommendTab = selectedTab.label == "推荐"
    val loadingTabLabel = if (selectedTab.label == "热门") "${selectedTab.label} · ${selectedHotTab.label}" else selectedTab.label
    val tabCacheKey = if (selectedTab.label == "热门") 1000 + selectedHotTabIndex else selectedTabIndex
    val currentTabPage = pageByCacheKey[tabCacheKey] ?: 1
    val currentTabEnded = endByCacheKey[tabCacheKey] == true
    val frostTarget = AppUiSettings.homeFrostStrength.coerceIn(0f, 1f)
    val frostStrength by animateFloatAsState(targetValue = frostTarget, label = "homeFrostStrength")
    val backgroundBlur = (frostStrength * 24f).dp
    val carouselDurationMs = (AppUiSettings.homeCarouselSeconds.coerceAtLeast(2) * 1000)

    LaunchedEffect(selectedTabIndex) {
        HomeScreenStateCache.selectedTabIndex = selectedTabIndex
        carouselControlMode = false
    }
    LaunchedEffect(selectedHotTabIndex) {
        HomeScreenStateCache.selectedHotTabIndex = selectedHotTabIndex
        tabLoadingMore = false
        if (selectedTab.label == "热门") {
            carouselControlMode = false
        }
    }
    LaunchedEffect(selectedIndex) {
        HomeScreenStateCache.selectedHeroIndex = selectedIndex
    }
    SideEffect {
        HomeScreenStateCache.tabCache.clear()
        HomeScreenStateCache.tabCache.putAll(tabCache)
    }

    LaunchedEffect(selectedTabIndex, selectedHotTabIndex) {
        if (selectedTab.opensSearch) return@LaunchedEffect
        if (selectedTab.label == "全部分区") {
            navController.navigate("categories")
            if (selectedTabIndex == HomeTabs.indexOfFirst { it.label == "全部分区" }) {
                selectedTabIndex = HomeTabs.indexOfFirst { it.label == "推荐" }.coerceAtLeast(0)
            }
            return@LaunchedEffect
        }
        tabError = null
        val cached = tabCache[tabCacheKey]
        if (!cached.isNullOrEmpty()) {
            tabVideos = cached
            val estimatedPage = ((cached.size - 1).coerceAtLeast(0) / 24) + 1
            pageByCacheKey.putIfAbsent(tabCacheKey, estimatedPage)
            endByCacheKey.putIfAbsent(tabCacheKey, cached.size < 24)
            selectedIndex = 0
            return@LaunchedEffect
        }
        loadingTab = true
        runCatching {
            if (selectedTab.label == "热门") {
                BiliRepository.fetchHotVideosByLabel(
                    label = selectedHotTab.label,
                    page = 1,
                    pageSize = 24
                )
            } else {
                BiliRepository.fetchHomeVideosByTabLabel(
                    tabLabel = selectedTab.label,
                    page = 1,
                    pageSize = 24
                )
            }
        }
            .onSuccess { fetched ->
                val resolved = fetched
                    .filter { it.id.isNotBlank() && it.title.isNotBlank() }
                    .distinctBy { it.id }
                if (resolved.isNotEmpty()) {
                    val normalized = normalizeHomeGridVideos(
                        source = resolved,
                        targetSize = 0
                    )
                    tabCache[tabCacheKey] = normalized
                    tabVideos = normalized
                    pageByCacheKey[tabCacheKey] = 1
                    endByCacheKey[tabCacheKey] = resolved.size < 24
                } else {
                    tabError = buildTabEmptyHint(selectedTab.label, authLoggedIn)
                    tabVideos = emptyList()
                    pageByCacheKey[tabCacheKey] = 1
                    endByCacheKey[tabCacheKey] = true
                }
                selectedIndex = 0
                refreshRequestKey = 0
            }
            .onFailure { throwable ->
                val friendly = buildTabEmptyHint(selectedTab.label, authLoggedIn)
                tabError = if (selectedTab.label in setOf("历史记录", "收藏", "我的")) {
                    if (authLoggedIn) {
                        throwable.message ?: friendly
                    } else {
                        friendly
                    }
                } else {
                    "“${selectedTab.label}” ${throwable.message ?: "加载失败"}"
                }
                tabVideos = tabCache[tabCacheKey].orEmpty()
                selectedIndex = 0
                refreshRequestKey = 0
            }
        loadingTab = false
    }

    LaunchedEffect(selectedTab.label, authLoggedIn) {
        if (selectedTab.label != "我的") return@LaunchedEffect
        if (!authLoggedIn) {
            myProfile = null
            myProfileLoading = false
            myProfileError = "请先扫码登录查看账号信息"
            return@LaunchedEffect
        }
        myProfileLoading = true
        myProfileError = null
        runCatching { BiliRepository.fetchCurrentUserProfile() }
            .onSuccess { profile ->
                myProfile = profile
                if (profile == null) {
                    myProfileError = "未获取到账号信息"
                }
            }
            .onFailure {
                myProfile = null
                myProfileError = it.message ?: "账号信息加载失败"
            }
        myProfileLoading = false
    }

    LaunchedEffect(qrDialogVisible) {
        if (!qrDialogVisible) {
            qrSession = null
            qrStatus = null
            return@LaunchedEffect
        }
        qrStatus = "正在生成登录二维码..."
        runCatching { BiliRepository.createQrLoginSession() }
            .onSuccess {
                qrSession = it
                qrStatus = "请使用哔哩哔哩 App 扫码登录"
            }
            .onFailure {
                qrStatus = it.message ?: "二维码生成失败"
            }
    }

    LaunchedEffect(qrDialogVisible, qrSession?.qrCodeKey) {
        val session = qrSession ?: return@LaunchedEffect
        if (!qrDialogVisible) return@LaunchedEffect
        while (qrDialogVisible) {
            val result = runCatching { BiliRepository.pollQrLogin(session.qrCodeKey) }.getOrNull()
            if (result == null) {
                qrStatus = "登录状态获取失败"
                delay(1500)
                continue
            }
            qrStatus = result.message
            when (result.state) {
                BiliRepository.QrPollState.SUCCESS -> {
                    authLoggedIn = BiliRepository.isLoggedIn()
                    detailActionHint = "登录成功"
                    delay(450)
                    qrDialogVisible = false
                    break
                }
                BiliRepository.QrPollState.EXPIRED -> break
                else -> delay(1400)
            }
        }
    }

    LaunchedEffect(detailActionHint) {
        if (!detailActionHint.isNullOrBlank()) {
            delay(1800)
            detailActionHint = null
        }
    }

    val heroVideos = remember(tabVideos) { tabVideos.take(16) }

    LaunchedEffect(heroVideos.size) {
        if (selectedIndex > heroVideos.lastIndex) selectedIndex = 0
    }

    val featured = heroVideos.getOrNull(selectedIndex.coerceIn(0, (heroVideos.size - 1).coerceAtLeast(0)))
    val recommendedRows = remember(tabVideos) { tabVideos.chunked(4) }
    val pagerProgress = remember { Animatable(0f) }
    val listState = rememberLazyListState()
    val recommendationTheme = RecommendationBackgroundThemes.resolve(AppUiSettings.recommendationFullscreenTheme)
    val recommendationFullscreenActive by remember {
        derivedStateOf {
            isRecommendTab && detailVideo == null && listState.firstVisibleItemIndex > 0
        }
    }
    val recommendationFullscreenAlpha by animateFloatAsState(
        targetValue = if (!isRecommendTab || recommendationFullscreenActive) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "recommendationFullscreenAlpha"
    )

    LaunchedEffect(selectedTabIndex, isRecommendTab, heroVideos.size, menuVisible, detailVideo != null, carouselControlMode) {
        while (true) {
            if (!isRecommendTab || heroVideos.isEmpty() || menuVisible || detailVideo != null || carouselControlMode) {
                pagerProgress.snapTo(0f)
                delay(150)
                continue
            }
            pagerProgress.snapTo(0f)
            pagerProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = carouselDurationMs, easing = LinearEasing)
            )
            selectedIndex = (selectedIndex + 1) % heroVideos.size
        }
    }

    LaunchedEffect(
        selectedTabIndex,
        isRecommendTab,
        tabVideos.size,
        tabVideos.firstOrNull()?.id,
        menuVisible,
        detailVideo?.id,
        qrDialogVisible,
        showExitConfirm
    ) {
        if (menuVisible || detailVideo != null || qrDialogVisible || showExitConfirm) return@LaunchedEffect
        val restoreId = HomeScreenStateCache.lastFocusedVideoId
        if (!restoreId.isNullOrBlank() && tabVideos.any { it.id == restoreId }) {
            pendingRestoreFocusVideoId = restoreId
            return@LaunchedEffect
        }
        delay(40)
        if (isRecommendTab) {
            runCatching { menuButtonFocusRequester.requestFocus() }
        } else if (tabVideos.isNotEmpty()) {
            runCatching { firstGridCardFocusRequester.requestFocus() }
        }
    }

    BackHandler(enabled = true) {
        when {
            qrDialogVisible -> qrDialogVisible = false
            detailVideo != null -> detailVideo = null
            menuVisible -> menuVisible = false
            showExitConfirm -> showExitConfirm = false
            else -> showExitConfirm = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (carouselControlMode && isRecommendTab && detailVideo == null && !menuVisible) {
                    when (keyEvent.key) {
                        Key.DirectionLeft -> {
                            if (heroVideos.isNotEmpty()) {
                                selectedIndex = (selectedIndex - 1).let { if (it < 0) heroVideos.lastIndex else it }
                                true
                            } else false
                        }
                        Key.DirectionRight -> {
                            if (heroVideos.isNotEmpty()) {
                                selectedIndex = (selectedIndex + 1) % heroVideos.size
                                true
                            } else false
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            featured?.let {
                                HomeScreenStateCache.lastFocusedVideoId = it.id
                                PlaybackQueueStore.setQueue(heroVideos.map { video -> video.id }, it.id)
                                navController.navigate("player/${it.id}")
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            carouselControlMode = false
                            runCatching { heroPlayFocusRequester.requestFocus() }
                            true
                        }
                        Key.DirectionDown -> {
                            carouselControlMode = false
                            scope.launch {
                                runCatching { listState.animateScrollToItem(1) }
                                runCatching { firstGridCardFocusRequester.requestFocus() }
                            }
                            true
                        }
                        Key.Back, Key.Escape -> {
                            carouselControlMode = false
                            true
                        }
                        else -> false
                    }.let { handled ->
                        if (handled) return@onPreviewKeyEvent true
                    }
                }
                when {
                    keyEvent.key == Key.Menu -> {
                        if (detailVideo == null) {
                            menuVisible = !menuVisible
                        }
                        true
                    }
                    keyEvent.key == Key.Back || keyEvent.key == Key.Escape -> {
                        when {
                            qrDialogVisible -> qrDialogVisible = false
                            detailVideo != null -> detailVideo = null
                            menuVisible -> menuVisible = false
                            showExitConfirm -> showExitConfirm = false
                            else -> showExitConfirm = true
                        }
                        true
                    }
                    menuVisible && keyEvent.key == Key.DirectionRight -> {
                        menuVisible = false
                        true
                    }
                    else -> false
                }
            }
    ) {
        if (isRecommendTab) {
            BiliNetImage(
                model = featured?.cover.orEmpty(),
                contentDescription = featured?.title ?: "首页背景",
                modifier = Modifier
                    .fillMaxSize()
                    .blur(backgroundBlur),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF050A13).copy(alpha = 0.78f * frostStrength),
                                Color(0xFF050A13).copy(alpha = 0.62f * frostStrength),
                                Color(0xFF050A13).copy(alpha = 0.38f * frostStrength)
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0xFF040912).copy(alpha = 0.30f * frostStrength),
                                0.42f to Color(0xFF040912).copy(alpha = 0.48f * frostStrength),
                                1.0f to Color(0xFF04070D).copy(alpha = 0.88f * frostStrength)
                            )
                        )
                    )
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = recommendationFullscreenAlpha }
                .background(
                    Brush.verticalGradient(
                        colors = recommendationTheme.main
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = recommendationFullscreenAlpha }
                .background(
                    Brush.radialGradient(
                        colors = recommendationTheme.glow,
                        radius = 1200f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = recommendationFullscreenAlpha * 0.65f }
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.09f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.14f)
                        )
                    )
                )
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 44.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item {
                Column {
                    if (detailVideo == null) {
                        HomeTopBar(
                            menuVisible = menuVisible,
                            currentTabLabel = selectedTab.label,
                            showLabel = !menuVisible,
                            menuButtonFocusRequester = menuButtonFocusRequester,
                            menuDownFocusRequester = if (isRecommendTab && detailVideo == null) heroPlayFocusRequester else null,
                            onToggleMenu = { menuVisible = !menuVisible },
                            onOpenSettings = { navController.navigate("settings") }
                        )
                    }

                    Spacer(modifier = Modifier.height(if (detailVideo == null && isRecommendTab) 228.dp else 24.dp))

                    if (detailVideo == null && isRecommendTab) {
                        Column(
                            modifier = Modifier.width(338.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box(
                                modifier = Modifier.height(66.dp),
                                contentAlignment = Alignment.BottomStart
                            ) {
                                Text(
                                    text = featured?.title ?: "暂无可展示内容",
                                    color = Color.White,
                                    fontSize = 23.sp,
                                    lineHeight = 28.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Box(
                                modifier = Modifier.height(18.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "UP主：${featured?.author?.ifBlank { "未知UP主" } ?: "--"}",
                                    color = Color(0xD7E1F4FF),
                                    fontSize = 13.sp,
                                    lineHeight = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            if (featured != null) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HomePlayButton(
                                        text = "播放",
                                        focusRequester = heroPlayFocusRequester,
                                        upFocusRequester = menuButtonFocusRequester,
                                        rightFocusRequester = heroInfoFocusRequester,
                                        onDirectionDown = {
                                            if (!carouselControlMode) {
                                                carouselControlMode = true
                                            } else {
                                                carouselControlMode = false
                                                scope.launch {
                                                    runCatching { listState.animateScrollToItem(1) }
                                                    runCatching { firstGridCardFocusRequester.requestFocus() }
                                                }
                                            }
                                        }
                                    ) {
                                        carouselControlMode = false
                                        HomeScreenStateCache.lastFocusedVideoId = featured.id
                                        PlaybackQueueStore.setQueue(heroVideos.map { video -> video.id }, featured.id)
                                        navController.navigate("player/${featured.id}")
                                    }
                                    HomeInfoButton(
                                        desc = "详情",
                                        focusRequester = heroInfoFocusRequester,
                                        upFocusRequester = menuButtonFocusRequester,
                                        leftFocusRequester = heroPlayFocusRequester,
                                        onDirectionDown = {
                                            if (!carouselControlMode) {
                                                carouselControlMode = true
                                            } else {
                                                carouselControlMode = false
                                                scope.launch {
                                                    runCatching { listState.animateScrollToItem(1) }
                                                    runCatching { firstGridCardFocusRequester.requestFocus() }
                                                }
                                            }
                                        }
                                    ) {
                                        carouselControlMode = false
                                        detailVideo = featured
                                        detailFollowed = null
                                        detailFollowLoading = false
                                        scope.launch {
                                            runCatching { BiliRepository.fetchVideoDetail(featured.id) }
                                                .onSuccess { resolved ->
                                                    if (resolved != null && detailVideo?.id == featured.id) {
                                                        detailVideo = resolved
                                                    }
                                                }
                                            if (authLoggedIn) {
                                                detailFollowLoading = true
                                                detailFollowed = runCatching { BiliRepository.isFollowingUpByVideo(featured.id) }
                                                    .getOrDefault(false)
                                                detailFollowLoading = false
                                            }
                                        }
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier.height(18.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                when {
                                    loadingTab -> Text(
                                        text = "正在加载 $loadingTabLabel ...",
                                        color = Color(0xC6F0F5FF),
                                        fontSize = 12.sp
                                    )

                                    !tabError.isNullOrBlank() -> Text(
                                        text = tabError.orEmpty(),
                                        color = Color(0xFFFFCE98),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            if (!tabError.isNullOrBlank() && tabError.orEmpty().contains("扫码登录")) {
                                Spacer(modifier = Modifier.height(4.dp))
                                HomeInfoButton(desc = "扫码登录") {
                                    qrDialogVisible = true
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(36.dp))

                    HomePagerDots(
                        count = heroVideos.size,
                        selected = selectedIndex,
                        progress = if (carouselControlMode) 1f else pagerProgress.value
                    ) { index ->
                        selectedIndex = index
                    }
                    } else if (detailVideo == null) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            when {
                                loadingTab -> Text(
                                    text = "正在加载 $loadingTabLabel ...",
                                    color = Color(0xC6F0F5FF),
                                    fontSize = 12.sp
                                )

                                !tabError.isNullOrBlank() -> Text(
                                    text = tabError.orEmpty(),
                                    color = Color(0xFFFFCE98),
                                    fontSize = 12.sp
                                )
                            }
                            if (!tabError.isNullOrBlank() && tabError.orEmpty().contains("扫码登录")) {
                                Spacer(modifier = Modifier.height(4.dp))
                                HomeInfoButton(desc = "扫码登录") {
                                    qrDialogVisible = true
                                }
                            }
                        }
                    }
                }
            }

            if (detailVideo == null) {
                if (selectedTab.label == "热门") {
                    item {
                        HotTabSelector(
                            tabs = HotTabs,
                            selectedIndex = selectedHotTabIndex,
                            onSelect = { index ->
                                val bounded = index.coerceIn(0, HotTabs.lastIndex)
                                if (bounded == selectedHotTabIndex) return@HotTabSelector
                                selectedHotTabIndex = bounded
                                refreshRequestKey = 0
                                HomeScreenStateCache.lastFocusedVideoId = null
                                pendingRestoreFocusVideoId = null
                            }
                        )
                    }
                }

                if (selectedTab.label == "我的") {
                    item {
                        MyAccountOverviewCard(
                            loggedIn = authLoggedIn,
                            profile = myProfile,
                            loading = myProfileLoading,
                            errorText = myProfileError,
                            onRequestLogin = { qrDialogVisible = true }
                        )
                    }
                }

                item {
                    HomeRecommendationGrid(
                        rows = recommendedRows,
                        refreshing = refreshingRecommendations,
                        loadingMore = tabLoadingMore,
                        firstCardFocusRequester = firstGridCardFocusRequester,
                        topExitFocusRequester = if (isRecommendTab) heroPlayFocusRequester else null,
                        restoreFocusVideoId = pendingRestoreFocusVideoId,
                        onRestoreFocusConsumed = {
                            pendingRestoreFocusVideoId = null
                            HomeScreenStateCache.lastFocusedVideoId = null
                        },
                        onReachEnd = {
                            if (!(loadingTab || refreshingRecommendations || tabLoadingMore || currentTabEnded)) {
                                scope.launch {
                                    tabLoadingMore = true
                                    val nextPage = currentTabPage + 1
                                    runCatching {
                                        if (selectedTab.label == "热门") {
                                            BiliRepository.fetchHotVideosByLabel(
                                                label = selectedHotTab.label,
                                                page = nextPage,
                                                pageSize = 24
                                            )
                                        } else {
                                            BiliRepository.fetchHomeVideosByTabLabel(
                                                tabLabel = selectedTab.label,
                                                page = nextPage,
                                                pageSize = 24
                                            )
                                        }
                                    }
                                        .onSuccess { latest ->
                                            if (latest.isEmpty()) {
                                                endByCacheKey[tabCacheKey] = true
                                            } else {
                                                val merged = (tabVideos + latest).distinctBy { it.id }
                                                if (merged.size == tabVideos.size) {
                                                    endByCacheKey[tabCacheKey] = true
                                                } else {
                                                    val normalized = normalizeHomeGridVideos(merged, targetSize = 0)
                                                    tabVideos = normalized
                                                    tabCache[tabCacheKey] = normalized
                                                    pageByCacheKey[tabCacheKey] = nextPage
                                                    endByCacheKey[tabCacheKey] = false
                                                }
                                            }
                                        }
                                        .onFailure {
                                            tabError = "自动加载失败，可继续浏览当前列表"
                                        }
                                    tabLoadingMore = false
                                }
                            }
                        },
                        onRefresh = {
                            if (!refreshingRecommendations) {
                                scope.launch {
                                    tabError = null
                                    refreshingRecommendations = true
                                    tabLoadingMore = false
                                    refreshRequestKey += 1
                                    runCatching {
                                        if (selectedTab.label == "热门") {
                                            BiliRepository.fetchHotVideosByLabel(
                                                label = selectedHotTab.label,
                                                page = 1,
                                                pageSize = 24,
                                                refreshKey = refreshRequestKey
                                            )
                                        } else {
                                            BiliRepository.fetchHomeVideosByTabLabel(
                                                tabLabel = selectedTab.label,
                                                page = 1,
                                                pageSize = 24,
                                                refreshKey = refreshRequestKey
                                            )
                                        }
                                    }
                                        .onSuccess { latest ->
                                            if (latest.isNotEmpty()) {
                                                val normalized = normalizeHomeGridVideos(
                                                    source = latest.distinctBy { it.id },
                                                    targetSize = 0
                                                )
                                                tabVideos = normalized
                                                tabCache[tabCacheKey] = normalized
                                                pageByCacheKey[tabCacheKey] = 1
                                                endByCacheKey[tabCacheKey] = latest.size < 24
                                            } else {
                                                pageByCacheKey[tabCacheKey] = 1
                                                endByCacheKey[tabCacheKey] = true
                                            }
                                        }
                                        .onFailure {
                                            tabError = "刷新失败，已展示当前推荐"
                                        }
                                    refreshingRecommendations = false
                                }
                            }
                        },
                        onClick = {
                            HomeScreenStateCache.lastFocusedVideoId = it.id
                            PlaybackQueueStore.setQueue(tabVideos.map { video -> video.id }, it.id)
                            navController.navigate("player/${it.id}")
                        }
                    )
                }
            }
        }

        detailVideo?.let { current ->
            DetailGlassOverlay(
                video = current,
                actionHint = detailActionHint,
                loggedIn = authLoggedIn,
                followed = detailFollowed,
                followLoading = detailFollowLoading,
                onRequestLogin = { qrDialogVisible = true },
                onLike = {
                    if (!authLoggedIn) {
                        detailActionHint = "请先扫码登录"
                        qrDialogVisible = true
                        return@DetailGlassOverlay
                    }
                    scope.launch {
                        detailActionHint = runCatching { BiliRepository.likeVideo(current.id) }
                            .getOrElse {
                                val msg = it.message ?: "点赞失败"
                                if (msg.contains("登录")) authLoggedIn = false
                                msg
                            }
                    }
                },
                onDislike = {
                    if (!authLoggedIn) {
                        detailActionHint = "请先扫码登录"
                        qrDialogVisible = true
                        return@DetailGlassOverlay
                    }
                    scope.launch {
                        detailActionHint = runCatching { BiliRepository.dislikeVideo(current.id) }
                            .getOrElse {
                                val msg = it.message ?: "操作失败"
                                if (msg.contains("登录")) authLoggedIn = false
                                msg
                            }
                    }
                },
                onCoin = {
                    if (!authLoggedIn) {
                        detailActionHint = "请先扫码登录"
                        qrDialogVisible = true
                        return@DetailGlassOverlay
                    }
                    scope.launch {
                        detailActionHint = runCatching { BiliRepository.coinVideo(current.id) }
                            .getOrElse {
                                val msg = it.message ?: "投币失败"
                                if (msg.contains("登录")) authLoggedIn = false
                                msg
                            }
                    }
                },
                onFavorite = {
                    if (!authLoggedIn) {
                        detailActionHint = "请先扫码登录"
                        qrDialogVisible = true
                        return@DetailGlassOverlay
                    }
                    scope.launch {
                        detailActionHint = runCatching { BiliRepository.favoriteVideo(current.id) }
                            .getOrElse {
                                val msg = it.message ?: "收藏失败"
                                if (msg.contains("登录")) authLoggedIn = false
                                msg
                            }
                    }
                },
                onToggleFollow = {
                    if (!authLoggedIn) {
                        detailActionHint = "请先扫码登录"
                        qrDialogVisible = true
                        return@DetailGlassOverlay
                    }
                    val target = !(detailFollowed ?: false)
                    scope.launch {
                        detailFollowLoading = true
                        detailActionHint = runCatching {
                            BiliRepository.toggleFollowUpByVideo(current.id, follow = target)
                        }.getOrElse {
                            val msg = it.message ?: "关注操作失败"
                            if (msg.contains("登录")) authLoggedIn = false
                            msg
                        }
                        detailFollowed = runCatching {
                            BiliRepository.isFollowingUpByVideo(current.id)
                        }.getOrDefault(target)
                        detailFollowLoading = false
                    }
                },
                onDismiss = { detailVideo = null },
                onPlay = {
                    detailVideo = null
                    val queue = tabVideos.takeIf { list -> list.any { it.id == current.id } } ?: heroVideos
                    PlaybackQueueStore.setQueue(queue.map { it.id }, current.id)
                    navController.navigate("player/${current.id}")
                }
            )
        }

        if (qrDialogVisible) {
            QrLoginOverlay(
                session = qrSession,
                statusText = qrStatus,
                onDismiss = { qrDialogVisible = false },
                onRefresh = {
                    scope.launch {
                        qrStatus = "正在刷新二维码..."
                        qrSession = runCatching { BiliRepository.createQrLoginSession() }
                            .onFailure { qrStatus = it.message ?: "二维码刷新失败" }
                            .getOrNull()
                        if (qrSession != null) {
                            qrStatus = "请使用哔哩哔哩 App 扫码登录"
                        }
                    }
                }
            )
        }

        if (menuVisible) {
            HomeMenuOverlay(
                selectedTab = selectedTabIndex,
                onDismiss = { menuVisible = false },
                onSelect = { index ->
                    menuVisible = false
                    carouselControlMode = false
                    val tab = HomeTabs[index]
                    if (tab.opensSearch) {
                        navController.navigate("search")
                    } else if (tab.label == "全部分区") {
                        navController.navigate("categories")
                    } else {
                        selectedTabIndex = index
                        selectedIndex = 0
                    }
                }
            )
        }

        if (showExitConfirm) {
            ExitConfirmOverlay(
                onDismiss = { showExitConfirm = false },
                onConfirm = { (context as? Activity)?.finish() }
            )
        }
    }
}

@Composable
private fun HomeTopBar(
    menuVisible: Boolean,
    currentTabLabel: String,
    showLabel: Boolean,
    menuButtonFocusRequester: FocusRequester,
    menuDownFocusRequester: FocusRequester?,
    onToggleMenu: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showLabel) {
            SingleTopButton(
                label = currentTabLabel,
                active = menuVisible,
                focusRequester = menuButtonFocusRequester,
                downFocusRequester = menuDownFocusRequester,
                onClick = onToggleMenu
            )
        } else {
            Spacer(modifier = Modifier.width(152.dp).height(44.dp))
        }
        HomeIconTopButton(icon = Icons.Filled.Settings, desc = "设置", onClick = onOpenSettings)
    }
}

@Composable
private fun HotTabSelector(
    tabs: List<HotTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "热门分区",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(tabs.size) { index ->
                val tab = tabs[index]
                HotTabChip(
                    tab = tab,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) }
                )
            }
        }
    }
}

@Composable
private fun HotTabChip(
    tab: HotTab,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val active = focused || selected
    val scale by animateFloatAsState(if (active) 1.03f else 1f, label = "hotTabScale")
    Box(
        modifier = Modifier
            .scale(scale)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && isConfirmKey(keyEvent.key)) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) Color(0x5ADCE8FC) else Color(0x1EFFFFFF))
            .border(
                BorderStroke(if (active) 2.dp else 1.dp, if (active) Color(0xFFE7EEFF) else Color(0x2EFFFFFF)),
                RoundedCornerShape(999.dp)
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = tab.symbol, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(text = tab.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun MyAccountOverviewCard(
    loggedIn: Boolean,
    profile: BiliRepository.UserProfile?,
    loading: Boolean,
    errorText: String?,
    onRequestLogin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0x1BFFFFFF))
            .border(BorderStroke(1.dp, Color(0x2DFFFFFF)), RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = "我的主页", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        when {
            loading -> {
                Text(text = "正在获取账号信息...", color = Color(0xFFD5E1F6), fontSize = 13.sp)
            }
            loggedIn && profile != null -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0x2BFFFFFF))
                            .border(BorderStroke(1.dp, Color(0x44FFFFFF)), CircleShape)
                    ) {
                        if (profile.avatar.isBlank()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = "UP", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            BiliNetImage(
                                model = profile.avatar,
                                contentDescription = profile.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = profile.name,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "UID ${profile.mid.ifBlank { "--" }} · Lv${profile.level} · 硬币 ${profile.coin}",
                            color = Color(0xFFC9D8EE),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            else -> {
                Text(text = errorText ?: "未登录", color = Color(0xFFD5E1F6), fontSize = 13.sp)
                HomeInfoButton(desc = "扫码登录") { onRequestLogin() }
            }
        }
    }
}

@Composable
private fun SingleTopButton(
    label: String,
    active: Boolean,
    focusRequester: FocusRequester,
    downFocusRequester: FocusRequester?,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val highlighted = focused || active
    Box(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusProperties {
                if (downFocusRequester != null) {
                    down = downFocusRequester
                }
            }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && isConfirmKey(keyEvent.key)) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .clip(RoundedCornerShape(999.dp))
            .background(if (highlighted) Color(0x60D8E4F8) else Color(0x2AFFFFFF))
            .border(
                BorderStroke(if (highlighted) 2.dp else 1.dp, if (highlighted) Color(0xFFE7EEFF) else Color(0x33FFFFFF)),
                RoundedCornerShape(999.dp)
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 9.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeIconTopButton(
    icon: ImageVector,
    desc: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "homeTopIconScale")
    Box(
        modifier = Modifier
            .size(42.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (focused) Color(0x60D8E4F8) else Color(0x2AFFFFFF))
            .border(BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFE7EEFF) else Color(0x33FFFFFF)), CircleShape)
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = desc,
            tint = Color.White,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun MenuHandle(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, label = "menuHandleScale")
    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (focused) Color(0x64FFFFFF) else Color(0x21000000))
            .border(BorderStroke(1.dp, if (focused) Color(0xFFF2F5FF) else Color(0x33FFFFFF)), CircleShape)
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "‹", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TabChip(label: String, active: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val highlighted = focused || active
    val scale by animateFloatAsState(if (highlighted) 1.04f else 1f, label = "tabChipScale")
    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(999.dp))
            .background(if (highlighted) Color(0x72172D57) else Color(0x4A111B2E))
            .border(
                BorderStroke(if (highlighted) 2.dp else 1.dp, if (highlighted) Color(0xFFD6E2FF) else Color(0x22FFFFFF)),
                RoundedCornerShape(999.dp)
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 9.dp)
    ) {
        Text(text = label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HeroPrimaryButton(text: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.05f else 1f, label = "heroPlayScale")
    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White)
            .border(
                BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFDCE8FF) else Color.Transparent),
                RoundedCornerShape(999.dp)
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = text,
                tint = Color(0xFF0B1320),
                modifier = Modifier.size(18.dp)
            )
            Text(text = text, color = Color(0xFF0B1320), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun HomePlayButton(
    text: String,
    focusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
    rightFocusRequester: FocusRequester? = null,
    onDirectionDown: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.04f else 1f, label = "homePlayScale")
    Box(
        modifier = Modifier
            .let { modifier ->
                if (focusRequester != null) modifier.focusRequester(focusRequester) else modifier
            }
            .focusProperties {
                if (upFocusRequester != null) up = upFocusRequester
                if (rightFocusRequester != null) right = rightFocusRequester
            }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    keyEvent.key == Key.DirectionDown && onDirectionDown != null -> {
                        onDirectionDown()
                        true
                    }
                    isConfirmKey(keyEvent.key) -> {
                        onClick()
                        true
                    }
                    else -> false
                }
            }
            .scale(scale)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White)
            .border(
                BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFDCE8FF) else Color.Transparent),
                RoundedCornerShape(999.dp)
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = text,
                tint = Color(0xFF0B1320),
                modifier = Modifier.size(15.dp)
            )
            Text(text = text, color = Color(0xFF0B1320), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun HomeInfoButton(
    desc: String,
    focusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
    leftFocusRequester: FocusRequester? = null,
    onDirectionDown: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.05f else 1f, label = "homeInfoScale")
    Box(
        modifier = Modifier
            .let { modifier ->
                if (focusRequester != null) modifier.focusRequester(focusRequester) else modifier
            }
            .focusProperties {
                if (upFocusRequester != null) up = upFocusRequester
                if (leftFocusRequester != null) left = leftFocusRequester
            }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    keyEvent.key == Key.DirectionDown && onDirectionDown != null -> {
                        onDirectionDown()
                        true
                    }
                    isConfirmKey(keyEvent.key) -> {
                        onClick()
                        true
                    }
                    else -> false
                }
            }
            .size(38.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (focused) Color(0x66DAE5FF) else Color(0x2A121D33))
            .border(
                BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFE8EEFF) else Color(0x28FFFFFF)),
                CircleShape
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = Icons.Filled.Info, contentDescription = desc, tint = Color.White, modifier = Modifier.size(15.dp))
    }
}

@Composable
private fun RefreshIconButton(
    refreshing: Boolean,
    upFocusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val infinite = rememberInfiniteTransition(label = "refreshSpin")
    val spinRaw by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "refreshSpinRaw"
    )
    val rotation by animateFloatAsState(
        targetValue = if (refreshing) spinRaw else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "refreshSpinAngle"
    )

    Box(
        modifier = Modifier
            .size(36.dp)
            .focusProperties {
                if (upFocusRequester != null) up = upFocusRequester
            }
            .clip(CircleShape)
            .background(
                when {
                    refreshing -> Color(0x35C9DDF8)
                    focused -> Color(0x44D8E5FF)
                    else -> Color(0x29101A2D)
                }
            )
            .border(
                BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFE8EEFF) else Color(0x34FFFFFF)),
                CircleShape
            )
            .focusable(interactionSource = interaction)
            .clickable(
                enabled = !refreshing,
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = "刷新",
            tint = Color.White,
            modifier = Modifier
                .size(18.dp)
                .rotate(rotation)
        )
    }
}

@Composable
private fun HeroCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.07f else 1f, label = "heroCircleScale")
    Box(
        modifier = Modifier
            .size(48.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (focused) Color(0x66DAE5FF) else Color(0x35121D33))
            .border(BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFE8EEFF) else Color(0x24FFFFFF)), CircleShape)
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = desc, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun HeroOutlineButton(
    text: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.05f else 1f, label = "heroOutlineScale")
    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(999.dp))
            .background(if (focused) Color(0x42FFFFFF) else Color(0x220B1422))
            .border(BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color.White else Color(0x32FFFFFF)), RoundedCornerShape(999.dp))
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HeroCircleTextButton(
    text: String,
    desc: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.07f else 1f, label = "heroCircleTextScale")
    Box(
        modifier = Modifier
            .size(48.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (focused) Color(0x66DAE5FF) else Color(0x35121D33))
            .border(BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFE8EEFF) else Color(0x24FFFFFF)), CircleShape)
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FeatureIntroOverlay(onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x73000000))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() }
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(1220.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Color(0xE8101A2A))
                .border(BorderStroke(1.dp, Color(0x35FFFFFF)), RoundedCornerShape(30.dp))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "功能介绍",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "主菜单与功能模块概览（按返回键可关闭）",
                        color = Color(0xC9D9E5FF),
                        fontSize = 14.sp
                    )
                }
                HeroCircleButton(icon = Icons.Filled.Close, desc = "关闭") { onDismiss() }
            }

            FeatureIntros.chunked(2).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    rowItems.forEach { item ->
                        FeatureIntroCard(item = item)
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.width(575.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureIntroCard(item: FeatureIntro) {
    Box(
        modifier = Modifier
            .width(575.dp)
            .height(116.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x1FFFFFFF))
            .border(BorderStroke(1.dp, Color(0x2AFFFFFF)), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.description,
                color = Color(0xD2DFEEFF),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomePagerDots(
    count: Int,
    selected: Int,
    progress: Float,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusProperties { canFocus = false },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count.coerceAtLeast(1)) { index ->
            val active = index == selected
            Box(
                modifier = Modifier
                    .focusProperties { canFocus = false }
                    .padding(horizontal = 4.dp)
                    .width(if (active) 34.dp else 8.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (active) Color.White.copy(alpha = 0.23f) else Color.White.copy(alpha = 0.38f))
                    .clickable { onSelect(index) }
            ) {
                if (active) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeRecommendationGrid(
    rows: List<List<VideoCard>>,
    refreshing: Boolean,
    loadingMore: Boolean = false,
    firstCardFocusRequester: FocusRequester?,
    topExitFocusRequester: FocusRequester?,
    restoreFocusVideoId: String?,
    onRestoreFocusConsumed: () -> Unit,
    onReachEnd: (() -> Unit)? = null,
    onRefresh: () -> Unit,
    onClick: (VideoCard) -> Unit
) {
    val restoreFocusRequester = remember { FocusRequester() }
    val hasRestoreTarget = remember(rows, restoreFocusVideoId) {
        !restoreFocusVideoId.isNullOrBlank() && rows.any { row -> row.any { it.id == restoreFocusVideoId } }
    }

    LaunchedEffect(restoreFocusVideoId, hasRestoreTarget, rows.size, refreshing) {
        if (restoreFocusVideoId == null) return@LaunchedEffect
        delay(36)
        if (hasRestoreTarget) {
            runCatching { restoreFocusRequester.requestFocus() }
        } else {
            runCatching { firstCardFocusRequester?.requestFocus() }
        }
        onRestoreFocusConsumed()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cardSpacing = 14.dp
        val cardWidth = (maxWidth - cardSpacing * 3) / 4
        val coverHeight = cardWidth * 0.56f
        val sectionShape = RoundedCornerShape(30.dp)
        val sectionTopColor = Color(0xFF162132).copy(alpha = 0.44f)
        val sectionBottomColor = Color(0xFF101B2B).copy(alpha = 0.56f)
        val sectionBorderColor = Color.White.copy(alpha = 0.34f)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(sectionShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(sectionTopColor, sectionBottomColor)
                    )
                )
                .border(BorderStroke(1.dp, sectionBorderColor), sectionShape)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RefreshIconButton(
                    refreshing = refreshing,
                    upFocusRequester = topExitFocusRequester,
                    onClick = onRefresh
                )
            }

            val totalItems = rows.sumOf { it.size }
            rows.forEachIndexed { rowIndex, rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(cardSpacing)) {
                    rowItems.forEachIndexed { itemIndex, item ->
                        val globalIndex = rowIndex * 4 + itemIndex
                        val nearEnd = onReachEnd != null && totalItems > 0 && globalIndex >= (totalItems - 8).coerceAtLeast(0)
                        HomeShelfCard(
                            item = item,
                            selected = false,
                            cardWidth = cardWidth,
                            coverHeight = coverHeight,
                            focusRequester = when {
                                !restoreFocusVideoId.isNullOrBlank() && item.id == restoreFocusVideoId -> restoreFocusRequester
                                rowIndex == 0 && itemIndex == 0 -> firstCardFocusRequester
                                else -> null
                            },
                            upFocusRequester = if (rowIndex == 0) topExitFocusRequester else null,
                            onFocused = if (nearEnd) onReachEnd else null,
                            onClick = { onClick(item) }
                        )
                    }
                    repeat((4 - rowItems.size).coerceAtLeast(0)) {
                        Spacer(modifier = Modifier.width(cardWidth))
                    }
                }
            }
            if (loadingMore) {
                Text(
                    text = "正在自动加载更多...",
                    color = Color(0xCFEAF3FF),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun HomeShelfCard(
    item: VideoCard,
    selected: Boolean,
    cardWidth: androidx.compose.ui.unit.Dp = 334.dp,
    coverHeight: androidx.compose.ui.unit.Dp = 186.dp,
    focusRequester: FocusRequester? = null,
    upFocusRequester: FocusRequester? = null,
    onFocused: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val active = focused || selected
    val border by animateColorAsState(
        if (active) Color(0xFFE0E9FF) else Color(0x20FFFFFF),
        label = "shelfCardBorder"
    )

    LaunchedEffect(focused) {
        if (focused && onFocused != null) {
            onFocused()
        }
    }

    val baseModifier = Modifier
            .width(cardWidth)
            .let { modifier ->
                if (focusRequester != null) modifier.focusRequester(focusRequester) else modifier
            }
            .focusProperties {
                if (upFocusRequester != null) up = upFocusRequester
            }
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)

    Column(
        modifier = baseModifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(coverHeight)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF131D30))
                    .border(BorderStroke(if (active) 2.dp else 1.dp, border), RoundedCornerShape(24.dp))
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
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.7f to Color.Transparent,
                                1.0f to Color(0xA8000000)
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "◉ ${item.viewCount}", color = Color.White, fontSize = 11.sp)
                    Text(text = "◍ ${item.danmakuCount}", color = Color.White, fontSize = 11.sp)
                }
                Text(
                    text = item.duration.ifBlank { "--:--" },
                    color = Color.White,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier.height(40.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Text(
                    text = item.title,
                    color = Color(0xFFF2F6FF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier.height(18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0x2CFFFFFF))
                            .border(BorderStroke(1.dp, Color(0x3CFFFFFF)), RoundedCornerShape(5.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "UP",
                            color = Color(0xFFEAF1FF),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "${item.author.ifBlank { "未知UP主" }} · ${formatHomeDate(item.publishDate)}",
                        color = Color(0xFFAFC0DD),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailGlassOverlay(
    video: VideoCard,
    actionHint: String?,
    loggedIn: Boolean,
    followed: Boolean?,
    followLoading: Boolean,
    onRequestLogin: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onCoin: () -> Unit,
    onFavorite: () -> Unit,
    onToggleFollow: () -> Unit,
    onDismiss: () -> Unit,
    onPlay: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val firstActionFocusRequester = remember(video.id) { FocusRequester() }
    var opened by remember(video.id) { mutableStateOf(false) }
    var closing by remember(video.id) { mutableStateOf(false) }

    val panelScaleX by animateFloatAsState(
        targetValue = if (opened) 1f else 0.88f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "detailPanelScaleX"
    )
    val panelScaleY by animateFloatAsState(
        targetValue = if (opened) 1f else 0.92f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "detailPanelScaleY"
    )
    val panelAlpha by animateFloatAsState(
        targetValue = if (opened) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "detailPanelAlpha"
    )
    val panelTranslateY by animateFloatAsState(
        targetValue = if (opened) 0f else -18f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "detailPanelOffsetY"
    )
    val scrimAlpha by animateFloatAsState(
        targetValue = if (opened) 0.28f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "detailScrimAlpha"
    )

    LaunchedEffect(video.id) {
        opened = true
        delay(80)
        runCatching { firstActionFocusRequester.requestFocus() }
    }

    fun dismissWithAnimation(action: (() -> Unit)? = null) {
        if (closing) return
        closing = true
        opened = false
        scope.launch {
            delay(190)
            if (action != null) action() else onDismiss()
        }
    }

    BackHandler(enabled = true) {
        dismissWithAnimation()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusProperties { canFocus = false }
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    dismissWithAnimation()
                }
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 26.dp, top = 172.dp)
                .width(374.dp)
                .heightIn(min = 264.dp, max = 360.dp)
                .graphicsLayer {
                    scaleX = panelScaleX
                    scaleY = panelScaleY
                    alpha = panelAlpha
                    translationY = panelTranslateY
                }
                .focusProperties { canFocus = false }
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xD129303A),
                            Color(0xC5262D36),
                            Color(0xCF29303A)
                        )
                    )
                )
                .border(BorderStroke(1.dp, Color(0x74FFFFFF)), RoundedCornerShape(30.dp))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.width(330.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier.height(18.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "UP主：${video.author.ifBlank { "未知UP主" }}",
                            color = Color(0xD7E1F4FF),
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = video.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        lineHeight = 23.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = video.description.ifBlank { "暂无简介" },
                    color = Color(0xD7E1F4FF),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OverlayInfoMetric(icon = Icons.Filled.PlayArrow, text = video.viewCount.ifBlank { "2629" })
                    OverlayInfoMetric(icon = Icons.Filled.Info, text = video.danmakuCount.ifBlank { "2" })
                    OverlayInfoMetric(icon = Icons.Filled.Info, text = formatHomeDate(video.publishDate))
                }

                if (!actionHint.isNullOrBlank()) {
                    Text(
                        text = actionHint,
                        color = Color(0xFFFFD9A3),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (!loggedIn) {
                    Text(
                        text = "未登录，点击任意互动按钮可扫码登录",
                        color = Color(0xFFC1CFE8),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onRequestLogin() }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                DetailRoundAction(
                    icon = Icons.Filled.PlayArrow,
                    value = "播放",
                    label = "播放",
                    focusRequester = firstActionFocusRequester
                ) {
                    dismissWithAnimation(onPlay)
                }
                DetailRoundAction(
                    icon = Icons.Filled.ThumbUp,
                    value = "点赞",
                    label = "点赞"
                ) { onLike() }
                DetailRoundAction(
                    icon = Icons.Filled.Close,
                    value = "不喜欢",
                    label = "不喜欢"
                ) { onDislike() }
                DetailRoundAction(
                    icon = Icons.Filled.Info,
                    glyph = "◎",
                    value = "投币",
                    label = "投币"
                ) { onCoin() }
                DetailRoundAction(
                    icon = Icons.Filled.Star,
                    value = "收藏",
                    label = "收藏"
                ) { onFavorite() }
                DetailAvatarRoundAction(
                    avatarUrl = video.avatar,
                    value = when {
                        !loggedIn -> "+关注"
                        followLoading -> "处理中"
                        followed == true -> "已关注 ✓"
                        else -> "+关注"
                    },
                    onClick = { onToggleFollow() }
                )
            }
        }
    }
}

@Composable
private fun OverlayInfoMetric(icon: ImageVector, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xEAF1FF),
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            color = Color(0xEAF1FF),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DetailPrimaryPlayAction(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.04f else 1f, label = "detailPrimaryPlayScale")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .width(112.dp)
                .height(44.dp)
                .scale(scale)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFF4F6FF))
                .border(
                    BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFE6EDFF) else Color(0x20FFFFFF)),
                    RoundedCornerShape(999.dp)
                )
                .focusable(interactionSource = interaction)
                .clickable(interactionSource = interaction, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "播放",
                tint = Color(0xFF0B1320),
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = "播放",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DetailAvatarAction(label: String) {
    Box(
        modifier = Modifier
            .width(50.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0x44FFFFFF))
                    .border(BorderStroke(1.dp, Color(0x5FFFFFFF)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DetailRoundAction(
    icon: ImageVector,
    iconRotation: Float = 0f,
    glyph: String? = null,
    value: String,
    label: String,
    focusRequester: FocusRequester? = null,
    onClick: (() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "detailRoundActionScale")

    Column(
        modifier = Modifier.width(50.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .scale(scale)
                .let { modifier ->
                    if (focusRequester != null) modifier.focusRequester(focusRequester) else modifier
                }
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown && isConfirmKey(keyEvent.key)) {
                        onClick?.invoke()
                        true
                    } else {
                        false
                    }
                }
                .clip(CircleShape)
                .background(if (focused) Color(0x62E5EDFA) else Color(0x2FFFFFFF))
                .border(
                    BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xEAF1FF) else Color(0x4AFFFFFF)),
                    CircleShape
                )
                .focusable(interactionSource = interaction)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = { onClick?.invoke() }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (glyph != null) {
                Text(
                    text = glyph,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(iconRotation)
                )
            }
        }
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DetailAvatarRoundAction(
    avatarUrl: String,
    value: String,
    focusRequester: FocusRequester? = null,
    onClick: (() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "detailAvatarActionScale")

    Column(
        modifier = Modifier.width(50.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .scale(scale)
                .let { modifier ->
                    if (focusRequester != null) modifier.focusRequester(focusRequester) else modifier
                }
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown && isConfirmKey(keyEvent.key)) {
                        onClick?.invoke()
                        true
                    } else {
                        false
                    }
                }
                .clip(CircleShape)
                .background(if (focused) Color(0x62E5EDFA) else Color(0x2FFFFFFF))
                .border(
                    BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xEAF1FF) else Color(0x4AFFFFFF)),
                    CircleShape
                )
                .focusable(interactionSource = interaction)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = { onClick?.invoke() }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl.isBlank()) {
                Text(
                    text = "UP",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                BiliNetImage(
                    model = avatarUrl,
                    contentDescription = "UP主头像",
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OverlayMetaChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0x26FFFFFF))
            .border(BorderStroke(1.dp, Color(0x2FFFFFFF)), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 13.sp)
    }
}

@Composable
private fun OverlayStatChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x1FFFFFFF))
            .border(BorderStroke(1.dp, Color(0x2BFFFFFF)), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(text = text, color = Color(0xFFE7EEFF), fontSize = 13.sp)
    }
}

@Composable
private fun OverlayActionButton(
    text: String,
    focusRequester: FocusRequester? = null,
    strong: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.05f else 1f, label = "overlayBtnScale")
    val bgColor by animateColorAsState(
        when {
            strong -> Color.White
            focused -> Color(0x44FFFFFF)
            else -> Color(0x22FFFFFF)
        },
        label = "overlayBtnBg"
    )

    Box(
        modifier = Modifier
            .let { modifier ->
                if (focusRequester != null) modifier.focusRequester(focusRequester) else modifier
            }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && isConfirmKey(keyEvent.key)) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .scale(scale)
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .border(
                BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color.White else Color(0x26FFFFFF)),
                RoundedCornerShape(999.dp)
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp)
    ) {
        Text(
            text = text,
            color = if (strong) Color(0xFF0B1422) else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun HomeMenuOverlay(
    selectedTab: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    val focusRequesters = remember { List(HomeTabs.size) { FocusRequester() } }
    val listState = rememberLazyListState()

    LaunchedEffect(selectedTab) {
        listState.scrollToItem(selectedTab.coerceIn(0, HomeTabs.lastIndex))
        delay(24)
        runCatching { focusRequesters[selectedTab.coerceIn(0, HomeTabs.lastIndex)].requestFocus() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusProperties { canFocus = false }
                .background(Color(0x76000000))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() }
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 18.dp)
                .width(214.dp)
                .fillMaxHeight(0.78f)
                .focusProperties { canFocus = false }
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xD129303A), Color(0xCA262D36), Color(0xCE29303A))
                    )
                )
                .border(BorderStroke(1.dp, Color(0x72FFFFFF)), RoundedCornerShape(24.dp))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(HomeTabs) { index, tab ->
                    SideMenuItem(
                        tab = tab,
                        selected = index == selectedTab,
                        focusRequester = focusRequesters[index],
                        onClick = { onSelect(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SideMenuItem(
    tab: HomeTab,
    selected: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val active = selected || focused
    val scale by animateFloatAsState(if (active) 1.03f else 1f, label = "sideItemScale")

    LaunchedEffect(selected) {
        if (selected) {
            delay(16)
            runCatching { focusRequester.requestFocus() }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && isConfirmKey(keyEvent.key)) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) Color(0x5CE6EEFC) else Color(0x1CFFFFFF))
            .border(
                BorderStroke(if (active) 2.dp else 1.dp, if (active) Color(0xFFE7EEFF) else Color(0x30FFFFFF)),
                RoundedCornerShape(999.dp)
            )
            .focusRequester(focusRequester)
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (active) Color(0x42FFFFFF) else Color(0x26FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = tab.symbol, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            text = tab.label,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

private fun isConfirmKey(key: Key): Boolean {
    return key == Key.Enter || key == Key.NumPadEnter || key == Key.DirectionCenter
}

@Composable
private fun QrLoginOverlay(
    session: BiliRepository.QrLoginSession?,
    statusText: String?,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    BackHandler(enabled = true) { onDismiss() }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x94000000))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() }
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(480.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xE61A2230))
                .border(BorderStroke(1.dp, Color(0x42FFFFFF)), RoundedCornerShape(24.dp))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "扫码登录", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = statusText.orEmpty().ifBlank { "请使用哔哩哔哩 App 扫码登录" },
                color = Color(0xFFCFE0FF),
                fontSize = 13.sp
            )

            Box(
                modifier = Modifier
                    .size(252.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x16000000))
                    .border(BorderStroke(1.dp, Color(0x32FFFFFF)), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (session != null) {
                    BiliNetImage(
                        model = session.qrImageUrl,
                        contentDescription = "扫码二维码",
                        modifier = Modifier
                            .size(232.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(text = "二维码加载中...", color = Color(0xFFD8E4F8), fontSize = 13.sp)
                }
            }

            if (!session?.loginUrl.isNullOrBlank()) {
                Text(
                    text = session?.loginUrl.orEmpty(),
                    color = Color(0xFF9FB4D8),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (session == null) {
                Text(
                    text = "二维码加载失败可点刷新重试",
                    color = Color(0xFFFFD39A),
                    fontSize = 12.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OverlayActionButton(text = "刷新二维码", strong = false, onClick = onRefresh)
                OverlayActionButton(text = "关闭", strong = false, onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun ExitConfirmOverlay(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val cancelFocusRequester = remember { FocusRequester() }
    BackHandler(enabled = true) { onDismiss() }

    LaunchedEffect(Unit) {
        delay(40)
        runCatching { cancelFocusRequester.requestFocus() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusProperties { canFocus = false }
                .background(Color(0x94000000))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() }
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(460.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xE61A2230))
                .border(BorderStroke(1.dp, Color(0x42FFFFFF)), RoundedCornerShape(24.dp))
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = "确定退出应用？", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text(text = "按返回可取消", color = Color(0xFFCFE0FF), fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OverlayActionButton(
                    text = "取消",
                    focusRequester = cancelFocusRequester,
                    strong = false,
                    onClick = onDismiss
                )
                OverlayActionButton(
                    text = "退出",
                    strong = true,
                    onClick = onConfirm
                )
            }
        }
    }
}

private fun formatHomeDate(value: String): String {
    if (value.isBlank()) return "日期未知"
    val normalized = value.replace("/", "-")
    return when {
        normalized.length >= 10 -> normalized.substring(5, 10)
        normalized.length > 5 -> normalized.substring(normalized.length - 5)
        else -> normalized
    }
}

private fun normalizeHomeGridVideos(source: List<VideoCard>, targetSize: Int = 16): List<VideoCard> {
    val base = source
        .filter { it.id.isNotBlank() && it.title.isNotBlank() }
        .distinctBy { it.id }
    return if (targetSize <= 0) base else base.take(targetSize)
}

private fun buildTabEmptyHint(tabLabel: String, loggedIn: Boolean): String {
    return when (tabLabel) {
        "历史记录" -> if (loggedIn) "还没有历史记录" else "还没有历史记录（登录后可同步）"
        "收藏" -> if (loggedIn) "还没有收藏内容" else "还没有收藏内容（登录后可同步）"
        "我的" -> if (loggedIn) "还没有发布视频" else "请先扫码登录查看我的主页"
        else -> "“$tabLabel” 暂无可展示内容"
    }
}
