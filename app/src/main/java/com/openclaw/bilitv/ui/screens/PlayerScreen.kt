package com.openclaw.bilitv.ui.screens

import android.view.KeyEvent as AndroidKeyEvent
import android.os.SystemClock
import android.view.MotionEvent
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.openclaw.bilitv.data.model.PlaybackInfo
import com.openclaw.bilitv.data.model.PlaybackQualityOption
import com.openclaw.bilitv.data.model.VideoComment
import com.openclaw.bilitv.data.repository.BiliRepository
import com.openclaw.bilitv.model.VideoCard
import com.openclaw.bilitv.ui.components.BiliNetImage
import com.openclaw.bilitv.ui.player.PlaybackQueueStore
import com.openclaw.bilitv.ui.player.PlayerKeyDispatcher
import com.openclaw.bilitv.ui.settings.AppUiSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private data class PlayerControlBridge(
    val togglePlayPause: () -> Boolean,
    val seekBy: (Long) -> Long,
    val restart: () -> Unit
)

private enum class PlayerKeyAction {
    TOGGLE_PLAY,
    SEEK_BACK,
    SEEK_FORWARD,
    TOGGLE_OVERLAY,
    BACK
}

private enum class OverlayFocusTarget {
    PLAY_PAUSE,
    AVATAR,
    PROGRESS
}

private fun mapComposeKeyAction(key: Key): PlayerKeyAction? {
    return when (key) {
        Key.Spacebar, Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> PlayerKeyAction.TOGGLE_PLAY
        Key.MediaRewind -> PlayerKeyAction.SEEK_BACK
        Key.MediaFastForward -> PlayerKeyAction.SEEK_FORWARD
        Key.Menu -> PlayerKeyAction.TOGGLE_OVERLAY
        Key.Back, Key.Escape -> PlayerKeyAction.BACK
        else -> null
    }
}

private fun mapAndroidKeyAction(keyCode: Int): PlayerKeyAction? {
    return when (keyCode) {
        AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        AndroidKeyEvent.KEYCODE_MEDIA_PLAY,
        AndroidKeyEvent.KEYCODE_MEDIA_PAUSE -> PlayerKeyAction.TOGGLE_PLAY
        AndroidKeyEvent.KEYCODE_MEDIA_REWIND -> PlayerKeyAction.SEEK_BACK
        AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> PlayerKeyAction.SEEK_FORWARD
        AndroidKeyEvent.KEYCODE_MENU -> PlayerKeyAction.TOGGLE_OVERLAY
        AndroidKeyEvent.KEYCODE_BACK,
        AndroidKeyEvent.KEYCODE_ESCAPE -> PlayerKeyAction.BACK
        else -> null
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(videoId: String, navController: NavController) {
    var video by remember(videoId) { mutableStateOf<VideoCard?>(null) }
    var related by remember(videoId) { mutableStateOf<List<VideoCard>>(emptyList()) }
    var comments by remember(videoId) { mutableStateOf<List<VideoComment>>(emptyList()) }

    var playbackInfo by remember(videoId) { mutableStateOf<PlaybackInfo?>(null) }
    var playbackLoading by remember(videoId) { mutableStateOf(true) }
    var playbackError by remember(videoId) { mutableStateOf<String?>(null) }
    var commentsLoading by remember(videoId) { mutableStateOf(true) }
    var commentsError by remember(videoId) { mutableStateOf<String?>(null) }

    var requestedQn by remember(videoId) { mutableStateOf<Int?>(null) }
    var qualityPanelVisible by remember { mutableStateOf(false) }
    var pagePanelVisible by remember { mutableStateOf(false) }
    var moreMenuVisible by remember { mutableStateOf(false) }
    var overlayPinned by remember { mutableStateOf(false) }
    var autoPlayEnabled by remember { mutableStateOf(true) }
    var singleLoopEnabled by remember { mutableStateOf(false) }
    var selectedNextIndex by remember(videoId) { mutableIntStateOf(0) }
    var actionHint by remember { mutableStateOf<String?>(null) }
    var authLoggedIn by remember { mutableStateOf(BiliRepository.isLoggedIn()) }
    var followState by remember(videoId) { mutableStateOf<Boolean?>(null) }
    var followLoading by remember(videoId) { mutableStateOf(false) }
    var playbackPages by remember(videoId) { mutableStateOf<List<BiliRepository.PlaybackPage>>(emptyList()) }
    var selectedPageIndex by remember(videoId) { mutableIntStateOf(0) }
    var selectedPageCid by remember(videoId) { mutableStateOf<Long?>(null) }
    var selectedPageTitle by remember(videoId) { mutableStateOf<String?>(null) }
    var selectedPageDurationSec by remember(videoId) { mutableStateOf<Long?>(null) }

    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var historyReporting by remember { mutableStateOf(false) }
    var lastHistoryReportBucket by remember(videoId, selectedPageCid) { mutableLongStateOf(-1L) }
    var playerControls by remember(videoId) { mutableStateOf<PlayerControlBridge?>(null) }
    val actionEntryFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }
    val progressFocusRequester = remember { FocusRequester() }
    var overlayVisible by remember { mutableStateOf(true) }
    var overlayFocusTarget by remember { mutableStateOf(OverlayFocusTarget.PLAY_PAUSE) }
    var overlayFocusToken by remember { mutableIntStateOf(0) }
    var overlayPulse by remember { mutableIntStateOf(0) }
    var danmakuEnabled by remember { mutableStateOf(true) }
    var danmakuMenuVisible by remember { mutableStateOf(false) }
    var followMenuVisible by remember { mutableStateOf(false) }
    var danmakuSamples by remember(videoId) { mutableStateOf<List<BiliRepository.DanmakuSample>>(emptyList()) }
    var danmakuLoading by remember(videoId) { mutableStateOf(false) }
    var danmakuError by remember(videoId) { mutableStateOf<String?>(null) }
    var danmakuCompact by remember { mutableStateOf(false) }
    var danmakuSpeedLevel by remember { mutableIntStateOf(1) }
    var danmakuOpacityLevel by remember { mutableIntStateOf(1) }
    var danmakuDensityLevel by remember { mutableIntStateOf(1) }
    var danmakuTextSizeLevel by remember { mutableIntStateOf(1) }
    var danmakuBackgroundEnabled by remember { mutableStateOf(false) }
    var danmakuReloadKey by remember(videoId) { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val overlayHideMs = AppUiSettings.playerOverlayAutoHideSeconds.coerceAtLeast(2) * 1000L
    var lastRemoteActionAt by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }

    LaunchedEffect(videoId) {
        PlaybackQueueStore.ensureCurrent(videoId)
        overlayVisible = false
        overlayFocusTarget = OverlayFocusTarget.PLAY_PAUSE
        overlayFocusToken = 0
        qualityPanelVisible = false
        pagePanelVisible = false
        danmakuMenuVisible = false
        moreMenuVisible = false
        followMenuVisible = false
        actionHint = null
        lastRemoteActionAt = SystemClock.uptimeMillis()

        authLoggedIn = BiliRepository.isLoggedIn()
        if (authLoggedIn) {
            followLoading = true
            followState = runCatching { BiliRepository.isFollowingUpByVideo(videoId) }.getOrDefault(false)
            followLoading = false
        } else {
            followState = false
        }

        runCatching { BiliRepository.fetchVideoDetail(videoId) }
            .onSuccess { video = it }

        runCatching { BiliRepository.fetchRelatedVideos(bvid = videoId, limit = 8) }
            .onSuccess { if (it.isNotEmpty()) related = it }

        runCatching { BiliRepository.fetchPlaybackPages(videoId) }
            .onSuccess { pages ->
                playbackPages = pages
                val initialIndex = pages.indexOfFirst { it.cid == selectedPageCid }.takeIf { it >= 0 } ?: 0
                selectedPageIndex = initialIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
                val selected = pages.getOrNull(selectedPageIndex)
                selectedPageCid = selected?.cid
                selectedPageTitle = selected?.title
                selectedPageDurationSec = selected?.durationSec
                if (pages.size > 1) {
                    actionHint = "检测到分P：${pages.size}"
                }
            }
            .onFailure {
                playbackPages = emptyList()
                selectedPageIndex = 0
                selectedPageCid = null
                selectedPageTitle = null
                selectedPageDurationSec = null
                actionHint = "分P加载失败: ${it.message ?: "unknown"}"
            }

        commentsLoading = true
        commentsError = null
        runCatching { BiliRepository.fetchVideoComments(videoId, page = 1, pageSize = 18) }
            .onSuccess { comments = it }
            .onFailure {
                comments = emptyList()
                commentsError = it.message ?: "评论加载失败"
            }
        commentsLoading = false
    }

    LaunchedEffect(videoId, selectedPageCid, danmakuReloadKey) {
        danmakuLoading = true
        danmakuError = null
        runCatching { BiliRepository.fetchDanmakuSamples(videoId, cid = selectedPageCid, limit = 260) }
            .onSuccess { loaded ->
                danmakuSamples = loaded
                if (loaded.isEmpty()) {
                    danmakuError = "当前视频未抓取到弹幕"
                }
            }
            .onFailure {
                danmakuSamples = emptyList()
                danmakuError = it.message ?: "弹幕加载失败"
            }
        danmakuLoading = false
    }

    LaunchedEffect(videoId, requestedQn, selectedPageCid) {
        playbackLoading = true
        playbackError = null
        playerControls = null
        runCatching {
            BiliRepository.fetchPlaybackInfo(
                bvid = videoId,
                preferredQn = requestedQn,
                targetCid = selectedPageCid,
                partTitle = selectedPageTitle,
                partDurationSec = selectedPageDurationSec
            )
        }
            .onSuccess {
                playbackInfo = it
                if (requestedQn == null) {
                    requestedQn = it.currentQn
                }
            }
            .onFailure {
                playbackError = it.message ?: "播放地址获取失败"
            }
        playbackLoading = false
    }

    LaunchedEffect(actionHint) {
        if (!actionHint.isNullOrBlank()) {
            delay(1400)
            actionHint = null
        }
    }

    LaunchedEffect(related.size) {
        if (selectedNextIndex > related.lastIndex) selectedNextIndex = 0
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) {
            overlayVisible = true
        }
    }

    LaunchedEffect(overlayVisible, overlayFocusToken, moreMenuVisible, danmakuMenuVisible, qualityPanelVisible, pagePanelVisible, followMenuVisible) {
        if (!overlayVisible || moreMenuVisible || danmakuMenuVisible || qualityPanelVisible || pagePanelVisible || followMenuVisible) return@LaunchedEffect
        delay(20)
        runCatching {
            when (overlayFocusTarget) {
                OverlayFocusTarget.PLAY_PAUSE -> playPauseFocusRequester.requestFocus()
                OverlayFocusTarget.AVATAR -> actionEntryFocusRequester.requestFocus()
                OverlayFocusTarget.PROGRESS -> progressFocusRequester.requestFocus()
            }
        }
    }

    LaunchedEffect(
        overlayVisible,
        isPlaying,
        qualityPanelVisible,
        pagePanelVisible,
        danmakuMenuVisible,
        moreMenuVisible,
        followMenuVisible,
        overlayHideMs,
        overlayPinned
    ) {
        if (!overlayVisible || qualityPanelVisible || pagePanelVisible || danmakuMenuVisible || moreMenuVisible || followMenuVisible || !isPlaying || overlayPinned) {
            return@LaunchedEffect
        }
        while (overlayVisible && !qualityPanelVisible && !pagePanelVisible && !danmakuMenuVisible && !moreMenuVisible && !followMenuVisible && isPlaying && !overlayPinned) {
            val idleMs = SystemClock.uptimeMillis() - lastRemoteActionAt
            if (idleMs >= overlayHideMs) {
                overlayVisible = false
                break
            }
            delay(120)
        }
    }

    fun wakeOverlay(target: OverlayFocusTarget? = null, requestFocus: Boolean = false) {
        if (target != null) {
            overlayFocusTarget = target
            if (requestFocus) {
                overlayFocusToken += 1
            }
        }
        overlayVisible = true
        overlayPulse += 1
        lastRemoteActionAt = SystemClock.uptimeMillis()
    }

    fun showMainOverlay(target: OverlayFocusTarget) {
        followMenuVisible = false
        qualityPanelVisible = false
        pagePanelVisible = false
        danmakuMenuVisible = false
        moreMenuVisible = false
        wakeOverlay(target = target, requestFocus = true)
    }

    fun isPlayerRoute(route: String?): Boolean {
        if (route.isNullOrBlank()) return false
        return route.startsWith("player")
    }

    fun exitPlayerScreen() {
        var guard = 0
        var popped = false
        while (isPlayerRoute(navController.currentBackStackEntry?.destination?.route) && guard < 20) {
            val ok = navController.popBackStack()
            if (!ok) break
            popped = true
            guard += 1
        }
        if (!popped || isPlayerRoute(navController.currentBackStackEntry?.destination?.route)) {
            navController.navigate("home") {
                launchSingleTop = true
                popUpTo(navController.graph.startDestinationId) { inclusive = false }
                restoreState = true
            }
        }
    }

    fun navigateToVideo(targetVideoId: String) {
        if (targetVideoId.isBlank()) return
        val currentDestinationId = navController.currentBackStackEntry?.destination?.id
        if (currentDestinationId != null) {
            navController.navigate("player/$targetVideoId") {
                launchSingleTop = true
                popUpTo(currentDestinationId) { inclusive = true }
            }
        } else {
            navController.navigate("player/$targetVideoId")
        }
    }

    fun reportWatchHistory(force: Boolean = false) {
        if (!authLoggedIn) return
        if (currentPositionMs < 4_000L) return
        if (historyReporting) return
        val currentSec = (currentPositionMs / 1000L).coerceAtLeast(0L)
        val bucket = if (force) currentSec else currentSec / 15L
        if (!force && bucket == lastHistoryReportBucket) return
        historyReporting = true
        scope.launch {
            val ok = runCatching {
                BiliRepository.reportWatchProgress(
                    bvid = videoId,
                    cid = selectedPageCid,
                    progressMs = currentPositionMs
                )
            }.getOrDefault(false)
            if (ok) {
                lastHistoryReportBucket = bucket
            }
            historyReporting = false
        }
    }

    fun playPreviousItemHidden(): Boolean {
        val previousPage = playbackPages.getOrNull(selectedPageIndex - 1)
        if (previousPage != null) {
            reportWatchHistory(force = true)
            selectedPageIndex -= 1
            selectedPageCid = previousPage.cid
            selectedPageTitle = previousPage.title
            selectedPageDurationSec = previousPage.durationSec
            actionHint = "上一集 P${previousPage.page}"
            return true
        }
        val previousVideoId = PlaybackQueueStore.previous(videoId)
        if (!previousVideoId.isNullOrBlank()) {
            navigateToVideo(previousVideoId)
            return true
        }
        actionHint = "这是第一个视频"
        return true
    }

    fun playNextItemHidden(): Boolean {
        val nextPage = playbackPages.getOrNull(selectedPageIndex + 1)
        if (nextPage != null) {
            reportWatchHistory(force = true)
            selectedPageIndex += 1
            selectedPageCid = nextPage.cid
            selectedPageTitle = nextPage.title
            selectedPageDurationSec = nextPage.durationSec
            actionHint = "下一集 P${nextPage.page}"
            return true
        }
        val nextVideoId = PlaybackQueueStore.next(videoId)
        if (!nextVideoId.isNullOrBlank()) {
            navigateToVideo(nextVideoId)
            return true
        }
        val nextVideo = related.firstOrNull()
        if (nextVideo != null) {
            navigateToVideo(nextVideo.id)
            return true
        }
        actionHint = "已经是最后一个视频"
        return true
    }

    fun dispatchAction(action: PlayerKeyAction): Boolean {
        when (action) {
            PlayerKeyAction.TOGGLE_OVERLAY -> {
                showMainOverlay(OverlayFocusTarget.AVATAR)
                return true
            }

            PlayerKeyAction.BACK -> {
                if (followMenuVisible) {
                    followMenuVisible = false
                    wakeOverlay()
                } else if (danmakuMenuVisible) {
                    danmakuMenuVisible = false
                    wakeOverlay()
                } else if (moreMenuVisible) {
                    moreMenuVisible = false
                    wakeOverlay()
                } else if (qualityPanelVisible) {
                    qualityPanelVisible = false
                    wakeOverlay()
                } else if (pagePanelVisible) {
                    pagePanelVisible = false
                    wakeOverlay()
                } else {
                    reportWatchHistory(force = true)
                    exitPlayerScreen()
                }
                return true
            }

            PlayerKeyAction.TOGGLE_PLAY -> {
                val bridge = playerControls ?: run {
                    actionHint = "播放器未就绪"
                    wakeOverlay()
                    return true
                }
                val nowPlaying = bridge.togglePlayPause()
                actionHint = if (nowPlaying) "继续播放" else "已暂停"
                wakeOverlay()
                return true
            }

            PlayerKeyAction.SEEK_BACK -> {
                val bridge = playerControls ?: run {
                    actionHint = "播放器未就绪"
                    wakeOverlay()
                    return true
                }
                val pos = bridge.seekBy(-10_000L)
                actionHint = "后退 10 秒  ${formatMs(pos)}"
                wakeOverlay()
                return true
            }

            PlayerKeyAction.SEEK_FORWARD -> {
                val bridge = playerControls ?: run {
                    actionHint = "播放器未就绪"
                    wakeOverlay()
                    return true
                }
                val pos = bridge.seekBy(10_000L)
                actionHint = "快进 10 秒  ${formatMs(pos)}"
                wakeOverlay()
                return true
            }
        }
    }

    DisposableEffect(videoId) {
        val registrationId = PlayerKeyDispatcher.register { event ->
            if (event.action != AndroidKeyEvent.ACTION_DOWN) return@register false
            lastRemoteActionAt = SystemClock.uptimeMillis()
            val menusClosed = !qualityPanelVisible && !pagePanelVisible && !danmakuMenuVisible && !moreMenuVisible && !followMenuVisible

            if (!overlayVisible && menusClosed) {
                when (event.keyCode) {
                    AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                        showMainOverlay(OverlayFocusTarget.PROGRESS)
                        return@register dispatchAction(PlayerKeyAction.SEEK_BACK)
                    }
                    AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                        showMainOverlay(OverlayFocusTarget.PROGRESS)
                        return@register dispatchAction(PlayerKeyAction.SEEK_FORWARD)
                    }
                    AndroidKeyEvent.KEYCODE_DPAD_CENTER,
                    AndroidKeyEvent.KEYCODE_ENTER,
                    AndroidKeyEvent.KEYCODE_NUMPAD_ENTER -> {
                        showMainOverlay(OverlayFocusTarget.PLAY_PAUSE)
                        return@register true
                    }
                    AndroidKeyEvent.KEYCODE_DPAD_UP -> return@register playPreviousItemHidden()
                    AndroidKeyEvent.KEYCODE_DPAD_DOWN -> return@register playNextItemHidden()
                }
            }

            if (event.keyCode == AndroidKeyEvent.KEYCODE_MENU) {
                return@register dispatchAction(PlayerKeyAction.TOGGLE_OVERLAY)
            }

            val overlayFocusMode = (overlayVisible || overlayPinned || !isPlaying) && menusClosed
            if (overlayFocusMode && event.keyCode in setOf(
                    AndroidKeyEvent.KEYCODE_DPAD_LEFT,
                    AndroidKeyEvent.KEYCODE_DPAD_RIGHT,
                    AndroidKeyEvent.KEYCODE_DPAD_UP,
                    AndroidKeyEvent.KEYCODE_DPAD_DOWN,
                    AndroidKeyEvent.KEYCODE_DPAD_CENTER,
                    AndroidKeyEvent.KEYCODE_ENTER,
                    AndroidKeyEvent.KEYCODE_NUMPAD_ENTER
                )
            ) {
                return@register false
            }

            val action = mapAndroidKeyAction(event.keyCode) ?: return@register false
            dispatchAction(action)
        }
        onDispose {
            reportWatchHistory(force = true)
            PlayerKeyDispatcher.clear(registrationId)
        }
    }

    LaunchedEffect(videoId, selectedPageCid, authLoggedIn, isPlaying) {
        while (true) {
            delay(15_000L)
            if (isPlaying && authLoggedIn) {
                reportWatchHistory(force = false)
            }
        }
    }

    BackHandler {
        dispatchAction(PlayerKeyAction.BACK)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                lastRemoteActionAt = SystemClock.uptimeMillis()
                val menusClosed = !qualityPanelVisible && !pagePanelVisible && !danmakuMenuVisible && !moreMenuVisible && !followMenuVisible

                if (!overlayVisible && menusClosed) {
                    when (keyEvent.key) {
                        Key.DirectionLeft -> {
                            showMainOverlay(OverlayFocusTarget.PROGRESS)
                            return@onPreviewKeyEvent dispatchAction(PlayerKeyAction.SEEK_BACK)
                        }
                        Key.DirectionRight -> {
                            showMainOverlay(OverlayFocusTarget.PROGRESS)
                            return@onPreviewKeyEvent dispatchAction(PlayerKeyAction.SEEK_FORWARD)
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            showMainOverlay(OverlayFocusTarget.PLAY_PAUSE)
                            return@onPreviewKeyEvent true
                        }
                        Key.DirectionUp -> return@onPreviewKeyEvent playPreviousItemHidden()
                        Key.DirectionDown -> return@onPreviewKeyEvent playNextItemHidden()
                        else -> Unit
                    }
                }

                if (keyEvent.key == Key.Menu) {
                    return@onPreviewKeyEvent dispatchAction(PlayerKeyAction.TOGGLE_OVERLAY)
                }

                val overlayFocusMode = (overlayVisible || overlayPinned || !isPlaying) && menusClosed
                if (overlayFocusMode && keyEvent.key in setOf(
                        Key.DirectionLeft,
                        Key.DirectionRight,
                        Key.DirectionUp,
                        Key.DirectionDown,
                        Key.DirectionCenter,
                        Key.Enter,
                        Key.NumPadEnter
                    )
                ) {
                    return@onPreviewKeyEvent false
                }

                val action = mapComposeKeyAction(keyEvent.key) ?: return@onPreviewKeyEvent false
                dispatchAction(action)
            }
    ) {
        when {
            playbackLoading -> PlayerLoadingPreview(item = video)
            playbackInfo != null -> RealPlayerSurface(
                info = playbackInfo!!,
                onPlayerUpdate = { position, duration, playing ->
                    currentPositionMs = position
                    totalDurationMs = duration
                    isPlaying = playing
                },
                onControlBridgeChanged = { playerControls = it },
                onAndroidKeyDown = { keyCode ->
                    val action = mapAndroidKeyAction(keyCode) ?: return@RealPlayerSurface false
                    dispatchAction(action)
                },
                onSurfaceTouchAction = { action ->
                    dispatchAction(action)
                },
                onPlaybackEnded = {
                    reportWatchHistory(force = true)
                    when {
                        singleLoopEnabled -> {
                            playerControls?.restart()
                            actionHint = "单视频循环"
                            wakeOverlay()
                        }
                        autoPlayEnabled -> {
                            val nextPage = playbackPages.getOrNull(selectedPageIndex + 1)
                            if (nextPage != null) {
                                selectedPageIndex += 1
                                selectedPageCid = nextPage.cid
                                selectedPageTitle = nextPage.title
                                selectedPageDurationSec = nextPage.durationSec
                                actionHint = "自动播放 P${nextPage.page}"
                                wakeOverlay()
                            } else {
                                val queueNextId = PlaybackQueueStore.next(videoId)
                                if (!queueNextId.isNullOrBlank()) {
                                    navigateToVideo(queueNextId)
                                } else {
                                    val next = related.firstOrNull()
                                    if (next != null) {
                                        navigateToVideo(next.id)
                                    }
                                }
                            }
                        }
                    }
                }
            )
            else -> PlayerErrorPreview(item = video, errorText = playbackError ?: "播放失败")
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusProperties { canFocus = false }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    wakeOverlay()
                }
        )

        val showOverlay = overlayPinned || overlayVisible || qualityPanelVisible || pagePanelVisible || moreMenuVisible || danmakuMenuVisible || followMenuVisible || !isPlaying || playbackLoading || !playbackError.isNullOrBlank()
        if (showOverlay) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                fun w(px: Float) = maxWidth * (px / 1920f)
                fun h(px: Float) = maxHeight * (px / 1080f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Transparent,
                                    0.55f to Color.Transparent,
                                    0.82f to Color(0x64000000),
                                    1f to Color(0xB8000000)
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0f to Color(0x76000000),
                                    0.34f to Color(0x34000000),
                                    1f to Color.Transparent
                                )
                            )
                        )
                )

                video?.let { currentVideo ->
                    PlayerInfoOverlay(
                        video = currentVideo,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .width(w(560f))
                            .padding(start = w(90f), end = w(24f), bottom = h(230f))
                    )
                }

                video?.let { currentVideo ->
                    PlayerActionOverlay(
                        video = currentVideo,
                        followText = when {
                            !authLoggedIn -> "+关注"
                            followLoading -> "处理中"
                            followState == true -> "已关注 ✓"
                            else -> "+关注"
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = w(94f), bottom = h(182f)),
                        firstFocusRequester = actionEntryFocusRequester,
                        onAction = { action ->
                            val targetBvid = currentVideo.id.ifBlank { videoId }
                            when (action) {
                                "follow" -> {
                                    followMenuVisible = !followMenuVisible
                                    qualityPanelVisible = false
                                    pagePanelVisible = false
                                    danmakuMenuVisible = false
                                    moreMenuVisible = false
                                }
                                "coin" -> scope.launch {
                                    if (!authLoggedIn) {
                                        actionHint = "请先在首页扫码登录"
                                        wakeOverlay()
                                        return@launch
                                    }
                                    actionHint = runCatching { BiliRepository.coinVideo(targetBvid) }
                                        .getOrElse {
                                            val msg = it.message ?: "投币失败"
                                            if (msg.contains("登录")) authLoggedIn = false
                                            msg
                                        }
                                }
                                "like" -> scope.launch {
                                    if (!authLoggedIn) {
                                        actionHint = "请先在首页扫码登录"
                                        wakeOverlay()
                                        return@launch
                                    }
                                    actionHint = runCatching { BiliRepository.likeVideo(targetBvid) }
                                        .getOrElse {
                                            val msg = it.message ?: "点赞失败"
                                            if (msg.contains("登录")) authLoggedIn = false
                                            msg
                                        }
                                }
                                "favorite" -> scope.launch {
                                    if (!authLoggedIn) {
                                        actionHint = "请先在首页扫码登录"
                                        wakeOverlay()
                                        return@launch
                                    }
                                    actionHint = runCatching { BiliRepository.favoriteVideo(targetBvid) }
                                        .getOrElse {
                                            val msg = it.message ?: "收藏失败"
                                            if (msg.contains("登录")) authLoggedIn = false
                                            msg
                                        }
                                }
                                "dislike" -> scope.launch {
                                    if (!authLoggedIn) {
                                        actionHint = "请先在首页扫码登录"
                                        wakeOverlay()
                                        return@launch
                                    }
                                    actionHint = runCatching { BiliRepository.dislikeVideo(targetBvid) }
                                        .getOrElse {
                                            val msg = it.message ?: "操作失败"
                                            if (msg.contains("登录")) authLoggedIn = false
                                            msg
                                        }
                                }
                                "danmaku" -> {
                                    moreMenuVisible = false
                                    qualityPanelVisible = false
                                    pagePanelVisible = false
                                    followMenuVisible = false
                                    danmakuMenuVisible = !danmakuMenuVisible
                                    actionHint = if (danmakuMenuVisible) "弹幕设置已打开" else "弹幕设置已关闭"
                                }
                                "more" -> {
                                    danmakuMenuVisible = false
                                    qualityPanelVisible = false
                                    pagePanelVisible = false
                                    followMenuVisible = false
                                    moreMenuVisible = !moreMenuVisible
                                    actionHint = if (moreMenuVisible) "已打开更多设置" else "已关闭更多设置"
                                }
                            }
                            wakeOverlay()
                        }
                    )
                }

                CinematicProgressOverlay(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = w(66f), end = w(66f), bottom = h(92f)),
                    positionMs = currentPositionMs,
                    durationMs = totalDurationMs,
                    isPlaying = isPlaying,
                    pageLabel = if (playbackPages.size > 1) {
                        "分P ${selectedPageIndex + 1}/${playbackPages.size}"
                    } else {
                        ""
                    },
                    showPageChip = playbackPages.size > 1,
                    qualityLabel = resolveQualityLabel(playbackInfo),
                    playPauseFocusRequester = playPauseFocusRequester,
                    progressFocusRequester = progressFocusRequester,
                    onPlayPause = { dispatchAction(PlayerKeyAction.TOGGLE_PLAY) },
                    onSeekBack = { dispatchAction(PlayerKeyAction.SEEK_BACK) },
                    onSeekForward = { dispatchAction(PlayerKeyAction.SEEK_FORWARD) },
                    onOpenPages = {
                        moreMenuVisible = false
                        danmakuMenuVisible = false
                        followMenuVisible = false
                        qualityPanelVisible = false
                        pagePanelVisible = !pagePanelVisible
                        actionHint = if (pagePanelVisible) "分P列表已打开" else "分P列表已关闭"
                        wakeOverlay()
                    },
                    onOpenQuality = {
                        moreMenuVisible = false
                        danmakuMenuVisible = false
                        followMenuVisible = false
                        pagePanelVisible = false
                        qualityPanelVisible = !qualityPanelVisible
                        actionHint = if (qualityPanelVisible) "清晰度设置已打开" else "清晰度设置已关闭"
                        wakeOverlay()
                    }
                )
            }
        }

        if (danmakuEnabled && danmakuSamples.isNotEmpty() && isPlaying) {
            DanmakuPreviewOverlay(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(top = 26.dp, start = 24.dp, end = 24.dp),
                danmakuList = danmakuSamples,
                currentPositionMs = currentPositionMs,
                compact = danmakuCompact,
                speedLevel = danmakuSpeedLevel,
                opacityLevel = danmakuOpacityLevel,
                densityLevel = danmakuDensityLevel,
                textSizeLevel = danmakuTextSizeLevel,
                backgroundEnabled = danmakuBackgroundEnabled
            )
        }

        if (showOverlay) {
            PlayerCloseRoundButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 26.dp, end = 24.dp),
                onClick = { dispatchAction(PlayerKeyAction.BACK) }
            )
        }

        if (qualityPanelVisible && playbackInfo != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 48.dp, bottom = 252.dp)
            ) {
                QualityMenu(
                    options = playbackInfo?.qualityOptions.orEmpty(),
                    currentQn = playbackInfo?.currentQn ?: 0,
                    onSelect = { option ->
                        requestedQn = option.qn
                        qualityPanelVisible = false
                        wakeOverlay()
                    }
                )
            }
        }

        if (pagePanelVisible && playbackPages.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 328.dp, bottom = 252.dp)
            ) {
                PageSelectMenu(
                    pages = playbackPages,
                    selectedPageIndex = selectedPageIndex,
                    onSelect = { index ->
                        val target = playbackPages.getOrNull(index) ?: return@PageSelectMenu
                        reportWatchHistory(force = true)
                        selectedPageIndex = index
                        selectedPageCid = target.cid
                        selectedPageTitle = target.title
                        selectedPageDurationSec = target.durationSec
                        actionHint = "切换到 P${target.page}：${target.title}"
                        pagePanelVisible = false
                        wakeOverlay()
                    },
                    onClose = { pagePanelVisible = false }
                )
            }
        }

        if (moreMenuVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 34.dp)
            ) {
                PlayerMoreMenu(
                    autoPlayEnabled = autoPlayEnabled,
                    singleLoopEnabled = singleLoopEnabled,
                    overlayPinned = overlayPinned,
                    pages = playbackPages,
                    selectedPageIndex = selectedPageIndex,
                    onToggleAutoPlay = {
                        autoPlayEnabled = !autoPlayEnabled
                        actionHint = if (autoPlayEnabled) "自动连播：开启" else "自动连播：关闭"
                    },
                    onToggleSingleLoop = {
                        singleLoopEnabled = !singleLoopEnabled
                        actionHint = if (singleLoopEnabled) "单视频循环：开启" else "单视频循环：关闭"
                    },
                    onToggleOverlayPinned = {
                        overlayPinned = !overlayPinned
                        actionHint = if (overlayPinned) "控件常驻：开启" else "控件常驻：关闭"
                        if (overlayPinned) {
                            overlayVisible = true
                        }
                    },
                    onSelectPage = { index ->
                        val target = playbackPages.getOrNull(index) ?: return@PlayerMoreMenu
                        reportWatchHistory(force = true)
                        selectedPageIndex = index
                        selectedPageCid = target.cid
                        selectedPageTitle = target.title
                        selectedPageDurationSec = target.durationSec
                        actionHint = "切换到 P${target.page}：${target.title}"
                        moreMenuVisible = false
                    },
                    onClose = { moreMenuVisible = false }
                )
            }
        }

        if (followMenuVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 34.dp)
            ) {
                FollowActionMenu(
                    loggedIn = authLoggedIn,
                    followed = followState == true,
                    loading = followLoading,
                    onRequestLogin = {
                        actionHint = "请先在首页扫码登录"
                    },
                    onToggleFollow = {
                        val targetBvid = video?.id?.ifBlank { videoId } ?: videoId
                        scope.launch {
                            if (!authLoggedIn) {
                                actionHint = "请先在首页扫码登录"
                                return@launch
                            }
                            if (followLoading) return@launch
                            followLoading = true
                            val target = !(followState ?: false)
                            val result = runCatching {
                                BiliRepository.toggleFollowUpByVideo(targetBvid, follow = target)
                            }
                            result.onSuccess {
                                actionHint = it
                                followState = runCatching {
                                    BiliRepository.isFollowingUpByVideo(targetBvid)
                                }.getOrDefault(target)
                            }.onFailure {
                                val msg = it.message ?: "关注失败"
                                if (msg.contains("登录")) {
                                    authLoggedIn = false
                                    followState = false
                                }
                                actionHint = msg
                            }
                            followLoading = false
                        }
                    },
                    onOpenUpSpace = {
                        val target = video?.id?.ifBlank { videoId } ?: videoId
                        followMenuVisible = false
                        navController.navigate("up/$target")
                    },
                    onClose = { followMenuVisible = false }
                )
            }
        }

        if (danmakuMenuVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 34.dp)
            ) {
                DanmakuMenu(
                    enabled = danmakuEnabled,
                    compact = danmakuCompact,
                    speedLevel = danmakuSpeedLevel,
                    opacityLevel = danmakuOpacityLevel,
                    densityLevel = danmakuDensityLevel,
                    textSizeLevel = danmakuTextSizeLevel,
                    backgroundEnabled = danmakuBackgroundEnabled,
                    loading = danmakuLoading,
                    count = danmakuSamples.size,
                    errorText = danmakuError,
                    onToggleEnabled = {
                        danmakuEnabled = !danmakuEnabled
                        actionHint = if (danmakuEnabled) "弹幕已开启" else "弹幕已关闭"
                    },
                    onToggleCompact = {
                        danmakuCompact = !danmakuCompact
                        actionHint = if (danmakuCompact) "弹幕模式：精简" else "弹幕模式：标准"
                    },
                    onCycleSpeed = {
                        danmakuSpeedLevel = (danmakuSpeedLevel + 1) % 3
                        actionHint = when (danmakuSpeedLevel) {
                            0 -> "弹幕速度：慢"
                            1 -> "弹幕速度：标准"
                            else -> "弹幕速度：快"
                        }
                    },
                    onCycleOpacity = {
                        danmakuOpacityLevel = (danmakuOpacityLevel + 1) % 3
                        actionHint = when (danmakuOpacityLevel) {
                            0 -> "弹幕透明度：低"
                            1 -> "弹幕透明度：中"
                            else -> "弹幕透明度：高"
                        }
                    },
                    onCycleDensity = {
                        danmakuDensityLevel = (danmakuDensityLevel + 1) % 3
                        actionHint = when (danmakuDensityLevel) {
                            0 -> "弹幕密度：低"
                            1 -> "弹幕密度：中"
                            else -> "弹幕密度：高"
                        }
                    },
                    onCycleTextSize = {
                        danmakuTextSizeLevel = (danmakuTextSizeLevel + 1) % 3
                        actionHint = when (danmakuTextSizeLevel) {
                            0 -> "弹幕字号：小"
                            1 -> "弹幕字号：标准"
                            else -> "弹幕字号：大"
                        }
                    },
                    onToggleBackground = {
                        danmakuBackgroundEnabled = !danmakuBackgroundEnabled
                        actionHint = if (danmakuBackgroundEnabled) "弹幕背景：开" else "弹幕背景：关"
                    },
                    onReload = { danmakuReloadKey += 1 },
                    onClose = { danmakuMenuVisible = false }
                )
            }
        }

        if (!actionHint.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 78.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xA0000000))
                    .border(BorderStroke(1.dp, Color(0x35FFFFFF)), RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = actionHint.orEmpty(),
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun PlayerInfoOverlay(video: VideoCard, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = video.author.ifBlank { "未知UP主" },
            color = Color(0xFFD4DEEC),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = video.title,
            color = Color.White,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 4,
            overflow = TextOverflow.Clip
        )
        Text(
            text = "${video.fansCount.ifBlank { "--粉丝" }}  ${video.duration.ifBlank { "--:--" }}  ${video.publishDate.ifBlank { "日期未知" }}",
            color = Color(0xFFE3E9F4),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = video.description.ifBlank { "暂无简介" },
            color = Color(0xCCEDF1F8),
            fontSize = 11.sp,
            maxLines = 3,
            overflow = TextOverflow.Clip
        )
        Text(
            text = "▶ ${video.viewCount.ifBlank { "--" }}   ▦ ${video.danmakuCount.ifBlank { "--" }}   ${video.likeCount.ifBlank { "--" }}",
            color = Color(0xFFF3F6FC),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PlayerActionOverlay(
    video: VideoCard,
    followText: String,
    modifier: Modifier = Modifier,
    firstFocusRequester: FocusRequester? = null,
    onAction: (String) -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        PlayerAvatarActionBubble(
            avatarUrl = video.avatar,
            label = followText,
            focusRequester = firstFocusRequester,
            onClick = { onAction("follow") }
        )
        PlayerIconActionBubble(glyph = "◎", value = "投币", label = "投币", onClick = { onAction("coin") })
        PlayerIconActionBubble(icon = Icons.Filled.ThumbUp, value = "点赞", label = "点赞", onClick = { onAction("like") })
        PlayerIconActionBubble(icon = Icons.Filled.Star, value = "收藏", label = "收藏", onClick = { onAction("favorite") })
        PlayerIconActionBubble(icon = Icons.Filled.Close, value = "不喜欢", label = "不喜欢", onClick = { onAction("dislike") })
        PlayerIconActionBubble(glyph = "弹", value = "弹幕", label = "弹幕", onClick = { onAction("danmaku") })
        PlayerIconActionBubble(glyph = "…", value = "更多", label = "更多", onClick = { onAction("more") })
    }
}

@Composable
private fun PlayerAvatarActionBubble(
    avatarUrl: String,
    label: String,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "playerAvatarActionScale")

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
                        onClick()
                        true
                    } else {
                        false
                    }
                }
                .clip(CircleShape)
                .background(if (focused) Color(0x62E5EDFA) else Color(0x32101722))
                .border(
                    BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFEAF1FF) else Color(0x54FFFFFF)),
                    CircleShape
                )
                .focusable(interactionSource = interaction)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick
                )
        ) {
            if (avatarUrl.isBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "UP", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                BiliNetImage(
                    model = avatarUrl,
                    contentDescription = "UP头像",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Text(text = label, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PlayerIconActionBubble(
    icon: ImageVector? = null,
    glyph: String? = null,
    value: String,
    label: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "playerIconActionScale")

    Column(
        modifier = Modifier.width(50.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .scale(scale)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown && isConfirmKey(keyEvent.key)) {
                        onClick()
                        true
                    } else {
                        false
                    }
                }
                .clip(CircleShape)
                .background(if (focused) Color(0x62E5EDFA) else Color(0x32101722))
                .border(
                    BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFEAF1FF) else Color(0x54FFFFFF)),
                    CircleShape
                )
                .focusable(interactionSource = interaction)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (glyph != null) {
                Text(
                    text = glyph,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Text(
            text = value.ifBlank { label },
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CinematicProgressOverlay(
    modifier: Modifier,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    pageLabel: String,
    showPageChip: Boolean,
    qualityLabel: String,
    playPauseFocusRequester: FocusRequester? = null,
    progressFocusRequester: FocusRequester? = null,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onOpenPages: () -> Unit,
    onOpenQuality: () -> Unit
) {
    val progress = if (durationMs > 0) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val progressInteraction = remember { MutableInteractionSource() }
    val progressFocused by progressInteraction.collectIsFocusedAsState()
    val playInteraction = remember { MutableInteractionSource() }
    val playFocused by playInteraction.collectIsFocusedAsState()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(11.dp)
                .let { current ->
                    if (progressFocusRequester != null) current.focusRequester(progressFocusRequester) else current
                }
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (keyEvent.key) {
                        Key.DirectionLeft -> {
                            onSeekBack()
                            true
                        }
                        Key.DirectionRight -> {
                            onSeekForward()
                            true
                        }
                        else -> false
                    }
                }
                .clip(RoundedCornerShape(999.dp))
                .background(if (progressFocused) Color(0x68121B28) else Color(0x5A111419))
                .border(
                    BorderStroke(
                        if (progressFocused) 2.dp else 1.dp,
                        if (progressFocused) Color(0xFFE8EEFF) else Color(0x30000000)
                    ),
                    RoundedCornerShape(999.dp)
                )
                .focusable(interactionSource = progressInteraction)
                .clickable(
                    interactionSource = progressInteraction,
                    indication = null,
                    onClick = onPlayPause
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(11.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFE12263))
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .let { current ->
                            if (playPauseFocusRequester != null) current.focusRequester(playPauseFocusRequester) else current
                        }
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown && isConfirmKey(keyEvent.key)) {
                                onPlayPause()
                                true
                            } else {
                                false
                            }
                        }
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (playFocused) Color(0x62E5EDFA) else Color(0x36FFFFFF))
                        .border(
                            BorderStroke(if (playFocused) 2.dp else 1.dp, if (playFocused) Color(0xFFEAF1FF) else Color(0x35FFFFFF)),
                            RoundedCornerShape(999.dp)
                        )
                        .focusable(interactionSource = playInteraction)
                        .clickable(
                            interactionSource = playInteraction,
                            indication = null,
                            onClick = onPlayPause
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isPlaying) "❚❚" else "▶",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = formatMs(positionMs),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (showPageChip) {
                    PlayerQualityRectChip(
                        label = pageLabel,
                        onClick = onOpenPages
                    )
                }
                PlayerQualityRectChip(
                    label = qualityLabel,
                    onClick = onOpenQuality
                )
                Text(
                    text = formatMs(durationMs),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PlayerQualityRectChip(
    label: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.04f else 1f, label = "playerQualityScale")
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
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) Color(0x62E5EDFA) else Color(0x36101722))
            .border(
                BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFEAF1FF) else Color(0x54FFFFFF)),
                RoundedCornerShape(10.dp)
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp)
    ) {
        Text(
            text = label.ifBlank { "--" },
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PlayerCloseRoundButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0x46000000))
            .border(BorderStroke(1.dp, Color(0x42FFFFFF)), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "关闭播放器",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun DanmakuPreviewOverlay(
    modifier: Modifier = Modifier,
    danmakuList: List<BiliRepository.DanmakuSample>,
    currentPositionMs: Long,
    compact: Boolean,
    speedLevel: Int,
    opacityLevel: Int,
    densityLevel: Int,
    textSizeLevel: Int,
    backgroundEnabled: Boolean
) {
    if (danmakuList.isEmpty()) return
    val laneCount = when {
        compact -> 1
        densityLevel <= 0 -> 2
        densityLevel == 1 -> 3
        else -> 4
    }
    val laneItemLimit = when {
        compact -> 1
        densityLevel <= 0 -> 2
        densityLevel == 1 -> 3
        else -> 4
    }
    val textAlpha = when (opacityLevel) {
        0 -> 0.68f
        2 -> 0.96f
        else -> 0.84f
    }
    val durationMs = when (speedLevel) {
        0 -> 12000L
        2 -> 7600L
        else -> 9200L
    }
    val spawnIntervalMs = when (densityLevel) {
        0 -> 190L
        1 -> 130L
        else -> 92L
    }
    val laneCooldownMs = when (densityLevel) {
        0 -> 880L
        1 -> 680L
        else -> 520L
    }
    val maxSpawnPerTick = when (densityLevel) {
        0 -> 1
        1 -> 2
        else -> 3
    }

    data class ActiveDanmaku(
        val text: String,
        val lane: Int,
        val slot: Int,
        val launchAtMs: Long
    )

    val active = remember(danmakuList, compact, speedLevel, densityLevel) {
        mutableStateListOf<ActiveDanmaku>()
    }
    var emittedCursor by remember(danmakuList) { mutableIntStateOf(0) }
    var lastPositionMs by remember(danmakuList) { mutableLongStateOf(-1L) }
    var frameClockMs by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    var lastSpawnAtMs by remember(danmakuList, compact, speedLevel, densityLevel) { mutableLongStateOf(0L) }
    val laneLastSpawnAt = remember(danmakuList, compact, speedLevel, densityLevel, laneCount) {
        LongArray(laneCount) { 0L }
    }
    var laneSeed by remember(danmakuList, compact, speedLevel, densityLevel) { mutableIntStateOf(0) }

    LaunchedEffect(danmakuList) {
        active.clear()
        emittedCursor = 0
        lastPositionMs = -1L
        lastSpawnAtMs = 0L
        laneLastSpawnAt.fill(0L)
        laneSeed = 0
    }

    LaunchedEffect(currentPositionMs, danmakuList, laneCount, laneItemLimit, durationMs, spawnIntervalMs, laneCooldownMs) {
        if (danmakuList.isEmpty()) return@LaunchedEffect
        val position = currentPositionMs.coerceAtLeast(0L)
        val now = SystemClock.uptimeMillis()
        frameClockMs = now
        active.removeAll { now - it.launchAtMs > durationMs + 3600L }

        // 进度跳跃时重置游标，确保弹幕从右侧重新发射。
        if (lastPositionMs < 0L || (position - lastPositionMs).absoluteValue > 2800L) {
            emittedCursor = danmakuList.indexOfFirst { it.timeMs >= position }
                .let { if (it < 0) danmakuList.size else it }
            active.clear()
            laneLastSpawnAt.fill(0L)
            laneSeed = 0
            lastSpawnAtMs = now
            lastPositionMs = position
            return@LaunchedEffect
        }

        val staleThresholdMs = position - 2200L
        while (emittedCursor < danmakuList.size && danmakuList[emittedCursor].timeMs < staleThresholdMs) {
            emittedCursor += 1
        }

        val dueUpperBound = position + 130L
        var spawned = 0
        while (emittedCursor < danmakuList.size && danmakuList[emittedCursor].timeMs <= dueUpperBound) {
            if (spawned >= maxSpawnPerTick) break
            if (now - lastSpawnAtMs < spawnIntervalMs) break
            val laneCounts = IntArray(laneCount)
            val usedSlots = Array(laneCount) { BooleanArray(laneItemLimit) }
            active.forEach { item ->
                if (item.lane in 0 until laneCount) {
                    laneCounts[item.lane] += 1
                    if (item.slot in 0 until laneItemLimit) usedSlots[item.lane][item.slot] = true
                }
            }
            var targetLane = -1
            var targetSlot = -1
            var minCount = Int.MAX_VALUE
            for (offset in 0 until laneCount) {
                val lane = (laneSeed + offset) % laneCount
                if (now - laneLastSpawnAt[lane] < laneCooldownMs) continue
                val laneActiveCount = laneCounts[lane]
                if (laneActiveCount >= laneItemLimit) continue
                val freeSlot = usedSlots[lane].indexOfFirst { used -> !used }
                if (freeSlot < 0) continue
                if (laneActiveCount < minCount) {
                    minCount = laneActiveCount
                    targetLane = lane
                    targetSlot = freeSlot
                }
            }
            if (targetLane < 0 || targetSlot < 0) break
            active.add(
                ActiveDanmaku(
                    text = danmakuList[emittedCursor].text,
                    lane = targetLane,
                    slot = targetSlot,
                    launchAtMs = now
                )
            )
            laneLastSpawnAt[targetLane] = now
            laneSeed = (targetLane + 1) % laneCount
            lastSpawnAtMs = now
            emittedCursor += 1
            spawned += 1
        }
        lastPositionMs = position
    }

    LaunchedEffect(active.size, durationMs) {
        while (active.isNotEmpty()) {
            val now = SystemClock.uptimeMillis()
            frameClockMs = now
            active.removeAll { now - it.launchAtMs > durationMs + 3600L }
            delay(16)
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val textSize = when (textSizeLevel) {
            0 -> if (compact) 12.sp else 14.sp
            2 -> if (compact) 16.sp else 18.sp
            else -> if (compact) 14.sp else 16.sp
        }
        val textPx = with(density) { textSize.toPx() }
        val laneSpacing = if (compact) 34.dp else 40.dp
        val baseTopOffset = if (compact) 6.dp else 6.dp

        if (active.isEmpty()) return@BoxWithConstraints

        active.forEach { item ->
            val progress = ((frameClockMs - item.launchAtMs).toFloat() / durationMs.toFloat()).coerceAtLeast(0f)
            if (progress > 1.7f) return@forEach
            val wideChars = item.text.count { it.code > 255 }
            val narrowChars = (item.text.length - wideChars).coerceAtLeast(0)
            val estimatedTextWidth =
                wideChars * textPx * 1.02f + narrowChars * textPx * 0.58f + if (backgroundEnabled) 66f else 48f
            val travel = widthPx + estimatedTextWidth
            val x = widthPx - travel * progress
            if (x < -estimatedTextWidth * 1.02f || x > widthPx + 14f) return@forEach

            Text(
                text = item.text,
                color = Color.White.copy(alpha = textAlpha),
                fontSize = textSize,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = baseTopOffset + laneSpacing * item.lane + (if (compact) 0.dp else 15.dp) * item.slot)
                    .graphicsLayer { translationX = x }
                    .then(
                        if (backgroundEnabled) {
                            Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0x2C000000))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}

@Composable
private fun DanmakuMenu(
    enabled: Boolean,
    compact: Boolean,
    speedLevel: Int,
    opacityLevel: Int,
    densityLevel: Int,
    textSizeLevel: Int,
    backgroundEnabled: Boolean,
    loading: Boolean,
    count: Int,
    errorText: String?,
    onToggleEnabled: () -> Unit,
    onToggleCompact: () -> Unit,
    onCycleSpeed: () -> Unit,
    onCycleOpacity: () -> Unit,
    onCycleDensity: () -> Unit,
    onCycleTextSize: () -> Unit,
    onToggleBackground: () -> Unit,
    onReload: () -> Unit,
    onClose: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .width(300.dp)
            .heightIn(max = 420.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xCE1F2732), Color(0xC01A212C), Color(0xC01C2531))
                )
            )
            .border(BorderStroke(1.dp, Color(0x56FFFFFF)), RoundedCornerShape(22.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        item {
            Text(
                text = "弹幕设置",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        item { PlayerMenuToggleItem(text = if (enabled) "关闭弹幕" else "开启弹幕", onClick = onToggleEnabled) }
        item { PlayerMenuToggleItem(text = if (compact) "模式：精简" else "模式：标准", onClick = onToggleCompact) }
        item {
            PlayerMenuToggleItem(
                text = "滚动速度：${when (speedLevel) { 0 -> "慢"; 2 -> "快"; else -> "标准" }}",
                onClick = onCycleSpeed
            )
        }
        item {
            PlayerMenuToggleItem(
                text = "透明度：${when (opacityLevel) { 0 -> "低"; 2 -> "高"; else -> "中" }}",
                onClick = onCycleOpacity
            )
        }
        item {
            PlayerMenuToggleItem(
                text = "密度：${when (densityLevel) { 0 -> "低"; 2 -> "高"; else -> "中" }}",
                onClick = onCycleDensity
            )
        }
        item {
            PlayerMenuToggleItem(
                text = "字号：${when (textSizeLevel) { 0 -> "小"; 2 -> "大"; else -> "标准" }}",
                onClick = onCycleTextSize
            )
        }
        item { PlayerMenuToggleItem(text = "文字背景：${if (backgroundEnabled) "开" else "关"}", onClick = onToggleBackground) }
        item { PlayerMenuToggleItem(text = "重新抓取弹幕", onClick = onReload) }
        item {
            Text(
                text = if (loading) "弹幕加载中..." else "已抓取弹幕 ${count} 条",
                color = Color(0xFFD5DEEE),
                fontSize = 12.sp
            )
        }
        if (!errorText.isNullOrBlank()) {
            item {
                Text(
                    text = errorText,
                    color = Color(0xFFFFD7A7),
                    fontSize = 11.sp
                )
            }
        }
        item { PlayerMenuToggleItem(text = "关闭菜单", onClick = onClose) }
    }
}

@Composable
private fun FollowActionMenu(
    loggedIn: Boolean,
    followed: Boolean,
    loading: Boolean,
    onRequestLogin: () -> Unit,
    onToggleFollow: () -> Unit,
    onOpenUpSpace: () -> Unit,
    onClose: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .width(300.dp)
            .heightIn(max = 320.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xCE1F2732), Color(0xC01A212C), Color(0xC01C2531))
                )
            )
            .border(BorderStroke(1.dp, Color(0x56FFFFFF)), RoundedCornerShape(22.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        item {
            Text(
                text = "UP 主操作",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        item {
            PlayerMenuToggleItem(
                text = when {
                    !loggedIn -> "登录后可关注"
                    loading -> "处理中..."
                    followed -> "取消关注"
                    else -> "关注 UP 主"
                },
                onClick = {
                    if (!loggedIn) {
                        onRequestLogin()
                    } else {
                        onToggleFollow()
                    }
                }
            )
        }
        item { PlayerMenuToggleItem(text = "查看主页", onClick = onOpenUpSpace) }
        item { PlayerMenuToggleItem(text = "关闭菜单", onClick = onClose) }
    }
}

@Composable
private fun PlayerMoreMenu(
    autoPlayEnabled: Boolean,
    singleLoopEnabled: Boolean,
    overlayPinned: Boolean,
    pages: List<BiliRepository.PlaybackPage>,
    selectedPageIndex: Int,
    onToggleAutoPlay: () -> Unit,
    onToggleSingleLoop: () -> Unit,
    onToggleOverlayPinned: () -> Unit,
    onSelectPage: (Int) -> Unit,
    onClose: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .width(360.dp)
            .heightIn(max = 460.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xCE1F2732), Color(0xC01A212C), Color(0xC01C2531))
                )
            )
            .border(BorderStroke(1.dp, Color(0x56FFFFFF)), RoundedCornerShape(22.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        item {
            Text(
                text = "更多设置",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        item { PlayerMenuToggleItem(text = "自动播放下一个：${if (autoPlayEnabled) "开" else "关"}", onClick = onToggleAutoPlay) }
        item { PlayerMenuToggleItem(text = "单视频循环：${if (singleLoopEnabled) "开" else "关"}", onClick = onToggleSingleLoop) }
        item { PlayerMenuToggleItem(text = "控件常驻：${if (overlayPinned) "开" else "关"}", onClick = onToggleOverlayPinned) }

        if (pages.size > 1) {
            item {
                Text(
                    text = "多 P 选集",
                    color = Color(0xFFD5DEEE),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            itemsIndexed(pages) { index, page ->
                val prefix = if (index == selectedPageIndex) "✓ " else ""
                PlayerMenuToggleItem(
                    text = "${prefix}P${page.page} ${page.title}  ${page.durationLabel}",
                    onClick = { onSelectPage(index) }
                )
            }
        }
        item { PlayerMenuToggleItem(text = "关闭菜单", onClick = onClose) }
    }
}

@Composable
private fun PageSelectMenu(
    pages: List<BiliRepository.PlaybackPage>,
    selectedPageIndex: Int,
    onSelect: (Int) -> Unit,
    onClose: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .width(360.dp)
            .heightIn(max = 460.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xCE1F2732), Color(0xC01A212C), Color(0xC01C2531))
                )
            )
            .border(BorderStroke(1.dp, Color(0x56FFFFFF)), RoundedCornerShape(22.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        item {
            Text(
                text = "分P选集",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        itemsIndexed(pages) { index, page ->
            val prefix = if (index == selectedPageIndex) "✓ " else ""
            PlayerMenuToggleItem(
                text = "${prefix}P${page.page} ${page.title}  ${page.durationLabel}",
                onClick = { onSelect(index) }
            )
        }
        item { PlayerMenuToggleItem(text = "关闭菜单", onClick = onClose) }
    }
}

@Composable
private fun PlayerMenuToggleItem(text: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && isConfirmKey(keyEvent.key)) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .clip(RoundedCornerShape(12.dp))
            .background(if (focused) Color(0x56D8E5FF) else Color(0x22FFFFFF))
            .border(
                BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFE8EEFF) else Color(0x2FFFFFFF)),
                RoundedCornerShape(12.dp)
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TouchControlBar(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onQuality: () -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0x4A10141B))
            .border(BorderStroke(1.dp, Color(0x30FFFFFF)), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TouchControlButton(text = "-10s", onClick = onSeekBack)
        TouchControlButton(text = if (isPlaying) "暂停" else "播放", strong = true, onClick = onPlayPause)
        TouchControlButton(text = "+10s", onClick = onSeekForward)
        TouchControlButton(text = "清晰度", onClick = onQuality)
        Spacer(modifier = Modifier.width(2.dp))
        TouchControlButton(text = "返回", onClick = onBack)
    }
}

@Composable
private fun TouchControlButton(
    text: String,
    strong: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (strong) Color(0xFFEFF3FF) else Color(0x32FFFFFF))
            .border(
                BorderStroke(1.dp, if (strong) Color.Transparent else Color(0x28FFFFFF)),
                RoundedCornerShape(999.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            text = text,
            color = if (strong) Color(0xFF0E1625) else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PlayerTopBar(onBack: () -> Unit, onOpenDetail: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = "融合播放",
                color = Color.White,
                fontSize = 31.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "播放器交互层 / 评论 / 连续播放",
                color = Color(0xFFB8C6DD),
                fontSize = 13.sp
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PlayerChip(text = "详情") { onOpenDetail() }
            PlayerChip(text = "返回") { onBack() }
        }
    }
}

@Composable
private fun PlayerHeroSection(
    video: VideoCard,
    info: PlaybackInfo?,
    loading: Boolean,
    errorText: String?,
    currentPositionMs: Long,
    totalDurationMs: Long,
    isPlaying: Boolean,
    qualityPanelVisible: Boolean,
    onToggleQuality: () -> Unit,
    onSelectQuality: (PlaybackQualityOption) -> Unit,
    onPlayerUpdate: (Long, Long, Boolean) -> Unit,
    onPlaybackEnded: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Color.Black)
            .border(BorderStroke(1.dp, Color(0x2CFFFFFF)), RoundedCornerShape(30.dp))
    ) {
        when {
            loading -> PlayerLoadingPreview(item = video)
            info != null -> RealPlayerSurface(
                info = info,
                onPlayerUpdate = onPlayerUpdate,
                onControlBridgeChanged = {},
                onAndroidKeyDown = { false },
                onSurfaceTouchAction = {},
                onPlaybackEnded = onPlaybackEnded
            )

            else -> PlayerErrorPreview(item = video, errorText = errorText ?: "播放失败")
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0x64000000))
                .border(BorderStroke(1.dp, Color(0x30FFFFFF)), RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = video.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(700.dp)
                )
                Text(
                    text = "${video.author} · ${video.publishDate.ifBlank { "日期未知" }} · ${video.duration}",
                    color = Color(0xFFD5DEEE),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End
        ) {
            PlayerChip(
                text = "清晰度 ${resolveQualityLabel(info)}",
                onClick = onToggleQuality
            )

            if (qualityPanelVisible) {
                QualityMenu(
                    options = info?.qualityOptions.orEmpty(),
                    currentQn = info?.currentQn ?: 0,
                    onSelect = onSelectQuality
                )
            }
        }

        ProgressOverlay(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            positionMs = currentPositionMs,
            durationMs = totalDurationMs,
            isPlaying = isPlaying,
            isLoading = loading
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun RealPlayerSurface(
    info: PlaybackInfo,
    onPlayerUpdate: (Long, Long, Boolean) -> Unit,
    onControlBridgeChanged: (PlayerControlBridge?) -> Unit,
    onAndroidKeyDown: (Int) -> Boolean,
    onSurfaceTouchAction: (PlayerKeyAction) -> Unit,
    onPlaybackEnded: () -> Unit
) {
    val context = LocalContext.current
    var playerError by remember { mutableStateOf<String?>(null) }

    val exoPlayer = remember(info.url, info.audioUrl) {
        val httpFactory = DefaultHttpDataSource.Factory().apply {
            setDefaultRequestProperties(info.headers)
            setUserAgent(info.headers["User-Agent"])
            setAllowCrossProtocolRedirects(true)
        }
        val sourceFactory = ProgressiveMediaSource.Factory(httpFactory)
        ExoPlayer.Builder(context)
            .build()
            .apply {
                val videoItem = MediaItem.Builder()
                    .setUri(info.url)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(info.title)
                            .setArtworkUri(android.net.Uri.parse(info.cover))
                            .build()
                    )
                    .build()

                val mediaSource = if (info.audioUrl.isNullOrBlank()) {
                    sourceFactory.createMediaSource(videoItem)
                } else {
                    val audioItem = MediaItem.Builder().setUri(info.audioUrl).build()
                    MergingMediaSource(
                        sourceFactory.createMediaSource(videoItem),
                        sourceFactory.createMediaSource(audioItem)
                    )
                }

                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                playWhenReady = true
                setMediaSource(mediaSource)
                prepare()
            }
    }

    DisposableEffect(exoPlayer) {
        onControlBridgeChanged(
            PlayerControlBridge(
                togglePlayPause = {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                    exoPlayer.isPlaying
                },
                seekBy = { deltaMs ->
                    val current = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val duration = exoPlayer.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                    val target = if (duration == Long.MAX_VALUE) {
                        (current + deltaMs).coerceAtLeast(0L)
                    } else {
                        (current + deltaMs).coerceIn(0L, duration)
                    }
                    exoPlayer.seekTo(target)
                    target
                },
                restart = {
                    exoPlayer.seekTo(0L)
                    exoPlayer.playWhenReady = true
                }
            )
        )
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playerError = error.errorCodeName + (error.message?.let { ": $it" } ?: "")
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onPlaybackEnded()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            onControlBridgeChanged(null)
            exoPlayer.release()
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            onPlayerUpdate(exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(0L), exoPlayer.isPlaying)
            delay(80)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    setKeepContentOnPlayerReset(true)
                    setBackgroundColor(android.graphics.Color.BLACK)
                    isClickable = true
                    isFocusable = false
                    isFocusableInTouchMode = false
                    setOnTouchListener { view, event ->
                        when (event.action) {
                            MotionEvent.ACTION_UP -> {
                                val width = view.width.toFloat().coerceAtLeast(1f)
                                val ratio = event.x / width
                                when {
                                    ratio < 0.3f -> onSurfaceTouchAction(PlayerKeyAction.SEEK_BACK)
                                    ratio > 0.7f -> onSurfaceTouchAction(PlayerKeyAction.SEEK_FORWARD)
                                    else -> onSurfaceTouchAction(PlayerKeyAction.TOGGLE_PLAY)
                                }
                                true
                            }
                            else -> true
                        }
                    }
                    this.player = exoPlayer
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(1.dp),
            update = {
                it.player = exoPlayer
                it.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        )

        if (playerError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xA0000000)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "播放器错误：$playerError",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun PlayerActionRow(item: VideoCard, onAction: (String) -> Unit) {
    val actions = listOf(
        "关注" to "已关注 ${item.author}",
        "点赞 ${item.likeCount}" to "已点赞",
        "投币" to "已投币（演示）",
        "弹幕" to "弹幕开关（演示）",
        "字幕" to "字幕开关（演示）",
        "更多" to "更多操作（演示）"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        actions.forEach { (label, hint) ->
            PlayerActionChip(
                text = label,
                onClick = { onAction(hint) }
            )
        }
    }
}

@Composable
private fun PlayerActionChip(text: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "playerActionScale")
    val bg by animateColorAsState(if (focused) Color(0x4AD8E5FF) else Color(0x1FFFFFFF), label = "playerActionBg")
    val border by animateColorAsState(if (focused) Color(0xFFE8EEFF) else Color(0x28FFFFFF), label = "playerActionBorder")

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(BorderStroke(if (focused) 2.dp else 1.dp, border), RoundedCornerShape(999.dp))
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CommentsPanel(
    modifier: Modifier,
    comments: List<VideoComment>,
    loading: Boolean,
    errorText: String?
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x2A0E1420))
            .border(BorderStroke(1.dp, Color(0x26FFFFFF)), RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "实时评论",
            color = Color.White,
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold
        )

        when {
            loading -> {
                Text(text = "评论加载中...", color = Color(0xFFC8D3E8), fontSize = 14.sp)
            }

            comments.isEmpty() -> {
                Text(
                    text = errorText ?: "暂无评论",
                    color = Color(0xFFC8D3E8),
                    fontSize = 14.sp
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 6.dp)
                ) {
                    items(comments, key = { it.id }) { comment ->
                        CommentCard(comment)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentCard(item: VideoComment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x16FFFFFF))
            .border(BorderStroke(1.dp, Color(0x1DFFFFFF)), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        BiliNetImage(
            model = item.avatar,
            contentDescription = item.author,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.author,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Lv${item.level}",
                    color = Color(0xFFADC2E6),
                    fontSize = 11.sp
                )
                Text(
                    text = item.publishTime,
                    color = Color(0xFF97A7C3),
                    fontSize = 11.sp
                )
            }
            Text(
                text = item.content,
                color = Color(0xFFD7E1F2),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${item.likeCount} · ${item.replyCount}条回复",
                color = Color(0xFF92A4C2),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ContinuePlayPanel(
    modifier: Modifier,
    related: List<VideoCard>,
    selectedNextIndex: Int,
    autoPlayEnabled: Boolean,
    onToggleAutoPlay: () -> Unit,
    onSelectNext: (Int) -> Unit,
    onPlayNow: (VideoCard) -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x2A0E1420))
            .border(BorderStroke(1.dp, Color(0x26FFFFFF)), RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "连续播放",
                color = Color.White,
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold
            )
            PlayerToggleChip(
                text = if (autoPlayEnabled) "自动连播: 开" else "自动连播: 关",
                enabled = autoPlayEnabled,
                onClick = onToggleAutoPlay
            )
        }

        if (related.isEmpty()) {
            Text(text = "暂无可连续播放内容", color = Color(0xFFC8D3E8), fontSize = 14.sp)
        } else {
            Text(
                text = "当前下一条：${related.getOrNull(selectedNextIndex)?.title.orEmpty()}",
                color = Color(0xFFD7E1F2),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            LazyColumn(
                modifier = Modifier.height(320.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 6.dp)
            ) {
                itemsIndexed(related) { index, item ->
                    ContinueItemCard(
                        item = item,
                        selected = index == selectedNextIndex,
                        onClick = {
                            onSelectNext(index)
                            onPlayNow(item)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContinueItemCard(item: VideoCard, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val active = focused || selected
    val border by animateColorAsState(if (active) Color(0xFFE2EAFF) else Color(0x25FFFFFF), label = "continueBorder")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(if (active) Color(0x3ED8E5FF) else Color(0x16FFFFFF))
            .border(BorderStroke(if (active) 2.dp else 1.dp, border), RoundedCornerShape(15.dp))
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF121927))
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
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${item.author} · ${item.duration}",
                color = Color(0xFF9EB1CF),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProgressOverlay(
    modifier: Modifier,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    isLoading: Boolean
) {
    val progress = if (durationMs > 0) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x66000000))
            .border(BorderStroke(1.dp, Color(0x26FFFFFF)), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isLoading) "缓冲中..." else if (isPlaying) "播放中" else "已暂停",
                color = Color(0xFFD4DDF0),
                fontSize = 12.sp
            )
            Text(
                text = "${formatMs(positionMs)} / ${formatMs(durationMs)}",
                color = Color(0xFFE6ECFF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0x2EFFFFFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun QualityMenu(
    options: List<PlaybackQualityOption>,
    currentQn: Int,
    onSelect: (PlaybackQualityOption) -> Unit
) {
    Box(
        modifier = Modifier
            .width(250.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xCE1F2732), Color(0xC01A212C), Color(0xC01C2531))
                )
            )
            .border(BorderStroke(1.dp, Color(0x56FFFFFF)), RoundedCornerShape(22.dp))
            .padding(vertical = 8.dp)
    ) {
        if (options.isEmpty()) {
            Text(
                text = "当前流未返回清晰度列表",
                color = Color(0xFFD5DEEE),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(options) { option ->
                    QualityItem(
                        option = option,
                        selected = option.qn == currentQn,
                        onClick = { onSelect(option) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QualityItem(option: PlaybackQualityOption, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val active = focused || selected

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && isConfirmKey(keyEvent.key)) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) Color(0x4AD8E5FF) else Color(0x16FFFFFF))
            .border(
                BorderStroke(if (active) 2.dp else 1.dp, if (active) Color(0xFFE7EEFF) else Color(0x20FFFFFF)),
                RoundedCornerShape(12.dp)
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = option.label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun PlayerLoadingPreview(item: VideoCard?) {
    Box(modifier = Modifier.fillMaxSize()) {
        BiliNetImage(
            model = item?.cover.orEmpty(),
            contentDescription = item?.title ?: "加载中",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = listOf(Color(0x45000000), Color(0xC0000000))))
        )
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "正在获取播放地址...",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (!item?.title.isNullOrBlank()) {
                Text(
                    text = item?.title.orEmpty(),
                    color = Color(0xFFD8E2F1),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(640.dp)
                )
            }
        }
    }
}

@Composable
private fun PlayerErrorPreview(item: VideoCard?, errorText: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        BiliNetImage(
            model = item?.cover.orEmpty(),
            contentDescription = item?.title ?: "播放失败",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = listOf(Color(0x56000000), Color(0xD0000000))))
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 30.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "播放失败", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text(text = errorText, color = Color(0xFFD7E0EF), fontSize = 14.sp)
        }
    }
}

@Composable
private fun PlayerChip(text: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "playerChipScale")
    val bg by animateColorAsState(if (focused) Color(0x5CD8E5FF) else Color(0x22FFFFFF), label = "playerChipBg")
    val border by animateColorAsState(if (focused) Color(0xFFE8EEFF) else Color(0x27FFFFFF), label = "playerChipBorder")

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
        Text(text = text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PlayerToggleChip(text: String, enabled: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (enabled) Color(0x48A5C8FF) else Color(0x24FFFFFF))
            .border(
                BorderStroke(
                    if (focused) 2.dp else 1.dp,
                    if (focused) Color(0xFFE8EEFF) else Color(0x24FFFFFF)
                ),
                RoundedCornerShape(999.dp)
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 12.sp)
    }
}

private fun resolveQualityLabel(info: PlaybackInfo?): String {
    if (info == null) return "--"
    return info.qualityOptions
        .firstOrNull { it.qn == info.currentQn }
        ?.label
        ?.substringBefore(" ")
        ?: "Q${info.currentQn}"
}

private fun formatMs(value: Long): String {
    if (value <= 0) return "00:00"
    val totalSec = value / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) {
        "%02d:%02d:%02d".format(h, m, s)
    } else {
        "%02d:%02d".format(m, s)
    }
}

private fun isConfirmKey(key: Key): Boolean {
    return key == Key.Enter || key == Key.NumPadEnter || key == Key.DirectionCenter
}
