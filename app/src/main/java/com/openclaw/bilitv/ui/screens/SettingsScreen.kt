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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.openclaw.bilitv.data.repository.BiliRepository
import com.openclaw.bilitv.ui.components.BiliNetImage
import com.openclaw.bilitv.ui.settings.AppUiSettings
import com.openclaw.bilitv.ui.settings.RecommendationBackgroundTheme
import com.openclaw.bilitv.ui.settings.RecommendationBackgroundThemes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var loggedIn by remember { mutableStateOf(BiliRepository.isLoggedIn()) }
    var userProfile by remember { mutableStateOf<BiliRepository.UserProfile?>(null) }
    var loadingProfile by remember { mutableStateOf(false) }
    var statusHint by remember { mutableStateOf<String?>(null) }

    var qrDialogVisible by remember { mutableStateOf(false) }
    var qrSession by remember { mutableStateOf<BiliRepository.QrLoginSession?>(null) }
    var qrStatus by remember { mutableStateOf<String?>(null) }

    fun refreshProfile() {
        scope.launch {
            if (!BiliRepository.isLoggedIn()) {
                loggedIn = false
                userProfile = null
                return@launch
            }
            loadingProfile = true
            runCatching { BiliRepository.fetchCurrentUserProfile() }
                .onSuccess {
                    userProfile = it
                    loggedIn = BiliRepository.isLoggedIn() && it != null
                }
                .onFailure {
                    loggedIn = BiliRepository.isLoggedIn()
                    if (loggedIn) {
                        statusHint = it.message ?: "获取用户信息失败"
                    }
                }
            loadingProfile = false
        }
    }

    BackHandler(enabled = qrDialogVisible) {
        qrDialogVisible = false
    }
    BackHandler(enabled = !qrDialogVisible) {
        navigateToHome(navController)
    }

    LaunchedEffect(Unit) {
        if (loggedIn) {
            refreshProfile()
        }
    }

    LaunchedEffect(statusHint) {
        if (!statusHint.isNullOrBlank()) {
            delay(1800)
            statusHint = null
        }
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
                delay(1400)
                continue
            }
            qrStatus = result.message
            when (result.state) {
                BiliRepository.QrPollState.SUCCESS -> {
                    loggedIn = true
                    refreshProfile()
                    statusHint = "登录成功"
                    delay(400)
                    qrDialogVisible = false
                    break
                }
                BiliRepository.QrPollState.EXPIRED -> break
                else -> delay(1200)
            }
        }
    }

    val frostPercent = (AppUiSettings.homeFrostStrength.coerceIn(0f, 1f) * 100f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0B111C), Color(0xFF0C131E), Color(0xFF080C14))
                )
            )
            .padding(horizontal = 44.dp),
        contentPadding = PaddingValues(top = 30.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "设置", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "显示与交互偏好", color = Color(0xFFC4D2E9), fontSize = 14.sp)
                }
                SettingsBackChip { navigateToHome(navController) }
            }
        }

        item {
            UserLoginCard(
                loggedIn = loggedIn,
                profile = userProfile,
                loading = loadingProfile,
                onLogin = { qrDialogVisible = true },
                onLogout = {
                    BiliRepository.clearLoginSession()
                    loggedIn = false
                    userProfile = null
                    statusHint = "已退出登录"
                }
            )
        }

        item {
            RecommendationThemeCard(
                theme = AppUiSettings.recommendationFullscreenTheme,
                onThemeSelected = { AppUiSettings.recommendationFullscreenTheme = it }
            )
        }

        item {
            SliderSettingCard(
                title = "主页磨砂强度",
                subtitle = "实时调节首页磨砂程度",
                valueText = "${frostPercent.roundToInt()}%"
            ) {
                RemoteSliderControl(
                    value = frostPercent.roundToInt().coerceIn(0, 100),
                    range = 0..100,
                    step = 1,
                    onValueChange = { AppUiSettings.homeFrostStrength = (it / 100f).coerceIn(0f, 1f) }
                )
            }
        }

        item {
            val seconds = AppUiSettings.homeCarouselSeconds.coerceIn(3, 15)
            SliderSettingCard(
                title = "主页轮播间隔",
                subtitle = "最小 3 秒，最大 15 秒",
                valueText = "${seconds}s"
            ) {
                RemoteSliderControl(
                    value = seconds,
                    range = 3..15,
                    step = 1,
                    onValueChange = { AppUiSettings.homeCarouselSeconds = it.coerceIn(3, 15) }
                )
            }
        }

        item {
            val seconds = AppUiSettings.playerOverlayAutoHideSeconds.coerceIn(3, 15)
            SliderSettingCard(
                title = "播放器控件停留时间",
                subtitle = "最小 3 秒，最大 15 秒",
                valueText = "${seconds}s"
            ) {
                RemoteSliderControl(
                    value = seconds,
                    range = 3..15,
                    step = 1,
                    onValueChange = { AppUiSettings.playerOverlayAutoHideSeconds = it.coerceIn(3, 15) }
                )
            }
        }

        if (!statusHint.isNullOrBlank()) {
            item {
                Text(
                    text = statusHint.orEmpty(),
                    color = Color(0xFFFFD9A3),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }

    if (qrDialogVisible) {
        SettingsQrLoginOverlay(
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
}

private fun navigateToHome(navController: NavController) {
    navController.navigate("home") {
        launchSingleTop = true
        popUpTo(navController.graph.startDestinationId) { inclusive = false }
        restoreState = true
    }
}

@Composable
private fun UserLoginCard(
    loggedIn: Boolean,
    profile: BiliRepository.UserProfile?,
    loading: Boolean,
    onLogin: () -> Unit,
    onLogout: () -> Unit
) {
    SettingsCard(title = "账号", subtitle = "扫码登录后可使用完整互动能力") {
        when {
            loading -> Text(text = "正在获取用户信息...", color = Color(0xFFE2E8F5), fontSize = 13.sp)
            loggedIn && profile != null -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0x2EFFFFFF))
                                .border(BorderStroke(1.dp, Color(0x56FFFFFF)), CircleShape)
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
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "UID ${profile.mid.ifBlank { "--" }} · Lv${profile.level} · 硬币 ${profile.coin}",
                                color = Color(0xFFC2D0E5),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    SettingsActionChip(text = "退出登录", danger = true, onClick = onLogout)
                }
            }
            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "当前未登录", color = Color(0xFFE2E8F5), fontSize = 13.sp)
                    SettingsActionChip(text = "登录", danger = false, onClick = onLogin)
                }
            }
        }
    }
}

@Composable
private fun RecommendationThemeCard(
    theme: Int,
    onThemeSelected: (Int) -> Unit
) {
    val options = RecommendationBackgroundThemes.all
    val selected = options.firstOrNull { it.id == theme } ?: options.first()
    SettingsCard(title = "推荐全屏背景样式", subtitle = "推荐区滚动到全屏时生效") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "当前样式",
                color = Color(0xFFB6C6DF),
                fontSize = 13.sp
            )
            Text(
                text = selected.label,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(options) { option ->
                SettingsThemeChip(
                    option = option,
                    selected = option.id == selected.id,
                    onClick = { onThemeSelected(option.id) }
                )
            }
        }
    }
}

@Composable
private fun SettingsThemeChip(
    option: RecommendationBackgroundTheme,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val active = selected || focused
    val scale by animateFloatAsState(if (active) 1.04f else 1f, label = "themeChipScale")
    val bg by animateColorAsState(
        if (selected) Color(0x58D7E3F9) else if (focused) Color(0x42FFFFFF) else Color(0x24FFFFFF),
        label = "themeChipBg"
    )
    val border by animateColorAsState(
        if (active) Color(0xFFDCE8FF) else Color(0x2DFFFFFF),
        label = "themeChipBorder"
    )
    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(BorderStroke(if (active) 2.dp else 1.dp, border), RoundedCornerShape(16.dp))
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(38.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(option.main))
                    .border(BorderStroke(1.dp, Color(0x36FFFFFF)), RoundedCornerShape(8.dp))
            )
            Text(text = option.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SliderSettingCard(
    title: String,
    subtitle: String,
    valueText: String,
    sliderContent: @Composable () -> Unit
) {
    SettingsCard(title = title, subtitle = subtitle) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "当前值", color = Color(0xFFB6C6DF), fontSize = 12.sp)
            Text(text = valueText, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(modifier = Modifier.padding(horizontal = 2.dp)) {
            sliderContent()
        }
    }
}

@Composable
private fun RemoteSliderControl(
    value: Int,
    range: IntRange,
    step: Int = 1,
    onValueChange: (Int) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val min = range.first
    val max = range.last
    val bounded = value.coerceIn(min, max)
    val fraction = if (max > min) {
        ((bounded - min).toFloat() / (max - min).toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val thumbFraction = fraction.coerceIn(0f, 1f).let { if (it <= 0f) 0.0001f else it }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (keyEvent.key) {
                        Key.DirectionLeft -> {
                            onValueChange((bounded - step).coerceIn(min, max))
                            true
                        }
                        Key.DirectionRight -> {
                            onValueChange((bounded + step).coerceIn(min, max))
                            true
                        }
                        else -> false
                    }
                }
                .clip(RoundedCornerShape(16.dp))
                .background(if (focused) Color(0x32DDE7FB) else Color(0x2211181F))
                .border(
                    BorderStroke(
                        if (focused) 2.dp else 1.dp,
                        if (focused) Color(0xFFE7EEFF) else Color(0x34FFFFFF)
                    ),
                    RoundedCornerShape(16.dp)
                )
                .focusable(interactionSource = interaction)
                .clickable(
                    interactionSource = interaction,
                    indication = null
                ) {}
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x2BFFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(thumbFraction)
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF9FD0FF), Color(0xFFE2EEFF))
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(thumbFraction)
                        .height(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF7FBFF))
                            .border(BorderStroke(1.dp, Color(0x660B1320)), CircleShape)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "$min", color = Color(0xFF9FB0C9), fontSize = 11.sp)
            Text(
                text = "← / → 调节",
                color = if (focused) Color(0xFFDDE8FB) else Color(0xFF8F9FB7),
                fontSize = 11.sp
            )
            Text(text = "$max", color = Color(0xFF9FB0C9), fontSize = 11.sp)
        }
    }
}

@Composable
private fun SettingsCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xB8141D2A), Color(0xAD121A26))
                )
            )
            .border(BorderStroke(1.dp, Color(0x45FFFFFF)), RoundedCornerShape(26.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = Color(0xFFB6C6DF), fontSize = 13.sp)
        }
        content()
    }
}

@Composable
private fun SettingsActionChip(
    text: String,
    danger: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.05f else 1f, label = "settingsActionScale")
    val bg by animateColorAsState(
        when {
            danger && focused -> Color(0x60FF6B6B)
            danger -> Color(0x35FF6B6B)
            focused -> Color(0x58D7E3F9)
            else -> Color(0x24FFFFFF)
        },
        label = "settingsActionBg"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(
                BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFE8EEFF) else Color(0x2AFFFFFF)),
                RoundedCornerShape(999.dp)
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SettingsBackChip(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "settingsBackScale")

    Box(
        modifier = Modifier
            .width(94.dp)
            .scale(scale)
            .clip(RoundedCornerShape(999.dp))
            .background(if (focused) Color(0x58D7E3F9) else Color(0x24FFFFFF))
            .border(BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFE8EEFF) else Color(0x2AFFFFFF)), RoundedCornerShape(999.dp))
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "返回", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SettingsQrLoginOverlay(
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
                .background(Color(0x8A000000))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() }
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(520.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xE01B2431), Color(0xDA18212D))
                    )
                )
                .border(BorderStroke(1.dp, Color(0x5EFFFFFF)), RoundedCornerShape(28.dp))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "扫码登录", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)

            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x1AFFFFFF))
                    .border(BorderStroke(1.dp, Color(0x2FFFFFFF)), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                val qrImage = session?.qrImageUrl.orEmpty()
                if (qrImage.isBlank()) {
                    Text(
                        text = "二维码加载中...",
                        color = Color(0xFFCAD6EB),
                        fontSize = 13.sp
                    )
                } else {
                    BiliNetImage(
                        model = qrImage,
                        contentDescription = "登录二维码",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Text(
                text = statusText ?: "请使用哔哩哔哩 App 扫码",
                color = Color(0xFFE2E9F6),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsActionChip(text = "刷新", danger = false, onClick = onRefresh)
                SettingsActionChip(text = "关闭", danger = true, onClick = onDismiss)
            }
        }
    }
}
