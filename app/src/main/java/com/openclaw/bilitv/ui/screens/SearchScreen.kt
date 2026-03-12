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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.openclaw.bilitv.data.repository.BiliRepository
import com.openclaw.bilitv.model.VideoCard
import com.openclaw.bilitv.ui.components.BiliNetImage
import com.openclaw.bilitv.ui.player.PlaybackQueueStore
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val KeyboardRows = listOf(
    listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
    listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
    listOf("Z", "X", "C", "V", "B", "N", "M")
)

private object SearchScreenStateCache {
    var lastFocusedVideoId: String? = null
}

@Composable
fun SearchScreen(navController: NavController) {
    var keywordInput by remember { mutableStateOf("") }
    var pendingKeyword by remember { mutableStateOf<String?>(null) }
    var defaultKeyword by remember { mutableStateOf("") }
    var hotKeywords by remember { mutableStateOf<List<String>>(emptyList()) }
    var suggestKeywords by remember { mutableStateOf<List<String>>(emptyList()) }

    val searchHistory = remember { mutableStateListOf<String>() }

    var results by remember { mutableStateOf<List<VideoCard>>(emptyList()) }
    var loadingResults by remember { mutableStateOf(false) }
    var loadingMoreResults by remember { mutableStateOf(false) }
    var loadingHot by remember { mutableStateOf(true) }
    var loadingSuggest by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(1) }
    var hasMore by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var systemInputVisible by remember { mutableStateOf(false) }
    var pendingRestoreFocusVideoId by remember { mutableStateOf(SearchScreenStateCache.lastFocusedVideoId) }
    val scope = rememberCoroutineScope()

    val firstKeyFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { BiliRepository.fetchSearchDefaultKeyword() }
            .onSuccess {
                defaultKeyword = it
            }

        runCatching { BiliRepository.fetchTrendingKeywords(limit = 12) }
            .onSuccess {
                hotKeywords = it
            }
            .onFailure {
                hotKeywords = emptyList()
            }

        if (defaultKeyword.isBlank()) {
            defaultKeyword = hotKeywords.firstOrNull().orEmpty().ifBlank { "输入关键词或拼音首字母" }
        }

        loadingHot = false
    }

    LaunchedEffect(keywordInput, hotKeywords, searchHistory.size) {
        val query = keywordInput.trim()
        if (query.isBlank()) {
            suggestKeywords = emptyList()
            loadingSuggest = false
            return@LaunchedEffect
        }
        delay(260)
        loadingSuggest = true
        val localFallback = (searchHistory + hotKeywords)
            .filter { it.contains(query, ignoreCase = true) }
            .distinct()

        val remote = runCatching { BiliRepository.fetchSearchSuggestions(query, limit = 12) }
            .getOrElse { emptyList() }
        suggestKeywords = if (remote.isNotEmpty()) {
            remote
        } else {
            localFallback.take(12)
        }

        loadingSuggest = false
    }

    LaunchedEffect(pendingKeyword) {
        val target = pendingKeyword?.trim().orEmpty()
        if (target.isBlank()) return@LaunchedEffect
        loadingResults = true
        loadingMoreResults = false
        errorText = null
        currentPage = 1
        hasMore = true

        runCatching { BiliRepository.searchVideos(keyword = target, page = 1, pageSize = 24) }
            .onSuccess { fetched ->
                val normalized = fetched
                    .filter { it.id.isNotBlank() && it.title.isNotBlank() }
                    .distinctBy { it.id }
                results = normalized
                hasMore = normalized.size >= 24
            }
            .onFailure {
                results = emptyList()
                hasMore = false
                errorText = "联网搜索失败，请稍后重试。"
            }

        loadingResults = false
    }

    BackHandler {
        if (systemInputVisible) {
            systemInputVisible = false
        } else {
            navigateToHome(navController)
        }
    }

    fun submitSearch(raw: String = keywordInput) {
        val query = raw.trim()
        if (query.isBlank()) return
        keywordInput = query
        pendingKeyword = query
        searchHistory.remove(query)
        searchHistory.add(0, query)
        if (searchHistory.size > 10) {
            searchHistory.removeRange(10, searchHistory.size)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF08111D), Color(0xFF090B12), Color(0xFF05070D))
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { keyEvent ->
                    if (systemInputVisible) return@onPreviewKeyEvent false
                    if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (keyEvent.key) {
                        Key.Back, Key.Escape -> {
                            navigateToHome(navController)
                            true
                        }
                        Key.Backspace -> {
                            if (keywordInput.isNotEmpty()) {
                                keywordInput = keywordInput.dropLast(1)
                            }
                            true
                        }
                        Key.Enter, Key.NumPadEnter -> {
                            submitSearch(keywordInput)
                            true
                        }
                        else -> {
                            val typed = keyToChar(keyEvent.key)
                            if (typed != null) {
                                keywordInput += typed
                                true
                            } else false
                        }
                    }
                }
                .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                SearchTopBar(
                    onBack = { navigateToHome(navController) },
                    onSearch = { submitSearch(keywordInput) },
                    onClose = { navigateToHome(navController) },
                    onSystemInput = { systemInputVisible = true }
                )
            }

        item {
            SearchInputBar(
                query = keywordInput,
                defaultKeyword = defaultKeyword,
            )
        }

        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth < 1500.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        KeyboardPanel(
                            modifier = Modifier.fillMaxWidth(),
                            firstKeyFocus = firstKeyFocus,
                            onType = { keywordInput += it },
                            onDelete = {
                                if (keywordInput.isNotEmpty()) {
                                    keywordInput = keywordInput.dropLast(1)
                                }
                            },
                            onSpace = { keywordInput += " " },
                            onClear = { keywordInput = "" },
                            onSearch = { submitSearch(keywordInput) }
                        )
                        SearchAssistPanel(
                            modifier = Modifier.fillMaxWidth(),
                            loading = loadingHot,
                            loadingSuggest = loadingSuggest,
                            history = searchHistory,
                            hotKeywords = hotKeywords,
                            suggestKeywords = suggestKeywords,
                            onPick = { picked ->
                                keywordInput = picked
                                submitSearch(picked)
                            }
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        KeyboardPanel(
                            modifier = Modifier.weight(1.25f),
                            firstKeyFocus = firstKeyFocus,
                            onType = { keywordInput += it },
                            onDelete = {
                                if (keywordInput.isNotEmpty()) {
                                    keywordInput = keywordInput.dropLast(1)
                                }
                            },
                            onSpace = { keywordInput += " " },
                            onClear = { keywordInput = "" },
                            onSearch = { submitSearch(keywordInput) }
                        )

                        SearchAssistPanel(
                            modifier = Modifier.weight(1f),
                            loading = loadingHot,
                            loadingSuggest = loadingSuggest,
                            history = searchHistory,
                            hotKeywords = hotKeywords,
                            suggestKeywords = suggestKeywords,
                            onPick = { picked ->
                                keywordInput = picked
                                submitSearch(picked)
                            }
                        )
                    }
                }
            }
        }

        if (!errorText.isNullOrBlank()) {
            item {
                Text(
                    text = errorText.orEmpty(),
                    color = Color(0xFFFFCF95),
                    fontSize = 13.sp
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pendingKeyword?.let { "“$it” 的结果" } ?: "输入关键词后按搜索",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (loadingResults) {
                    CircularProgressIndicator(
                        color = Color(0xFFDDE6FF),
                        modifier = Modifier.height(26.dp)
                    )
                }
            }
        }

        item {
            SearchResultGrid(
                items = results,
                searched = !pendingKeyword.isNullOrBlank(),
                loadingMore = loadingMoreResults,
                hasMore = hasMore,
                restoreFocusVideoId = pendingRestoreFocusVideoId,
                onRestoreFocusConsumed = {
                    pendingRestoreFocusVideoId = null
                    SearchScreenStateCache.lastFocusedVideoId = null
                },
                onReachEnd = {
                    val query = pendingKeyword?.trim().orEmpty()
                    if (!(query.isBlank() || loadingResults || loadingMoreResults || !hasMore)) {
                        scope.launch {
                            loadingMoreResults = true
                            val nextPage = currentPage + 1
                            runCatching {
                                BiliRepository.searchVideos(keyword = query, page = nextPage, pageSize = 24)
                            }
                                .onSuccess { latest ->
                                    if (latest.isEmpty()) {
                                        hasMore = false
                                    } else {
                                        val merged = (results + latest).distinctBy { it.id }
                                        if (merged.size == results.size) {
                                            hasMore = false
                                        } else {
                                            results = merged
                                            currentPage = nextPage
                                            hasMore = latest.size >= 24
                                        }
                                    }
                                }
                                .onFailure {
                                    errorText = "搜索加载更多失败，请稍后重试。"
                                }
                            loadingMoreResults = false
                        }
                    }
                },
                onOpen = {
                    SearchScreenStateCache.lastFocusedVideoId = it.id
                    PlaybackQueueStore.setQueue(results.map { video -> video.id }, it.id)
                    navController.navigate("player/${it.id}")
                }
            )
        }
        }

        if (systemInputVisible) {
            SearchSystemInputOverlay(
                initialValue = keywordInput,
                onDismiss = { systemInputVisible = false },
                onConfirm = { text ->
                    keywordInput = text
                    systemInputVisible = false
                    submitSearch(text)
                }
            )
        }
    }
}

@Composable
private fun SearchTopBar(
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onClose: () -> Unit,
    onSystemInput: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("智能全局搜索", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "屏幕键盘 / 热搜 / 历史记录 / 一键直达",
                color = Color(0xFFB5C3DB),
                fontSize = 13.sp
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SearchChip(text = "搜索") { onSearch() }
            SearchChip(text = "系统输入") { onSystemInput() }
            SearchChip(text = "返回") { onBack() }
            SearchChip(text = "关闭") { onClose() }
        }
    }
}

@Composable
private fun SearchInputBar(
    query: String,
    defaultKeyword: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0x1CFFFFFF))
            .border(BorderStroke(1.dp, Color(0x24FFFFFF)), RoundedCornerShape(26.dp))
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = if (query.isBlank()) defaultKeyword.ifBlank { "输入关键词" } else query,
                color = if (query.isBlank()) Color(0xFF95A5C2) else Color(0xFFF2F6FF),
                fontSize = 23.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "默认词仅为提示，不会自动填入",
                color = Color(0xFFA5B3CC),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun KeyboardPanel(
    modifier: Modifier,
    firstKeyFocus: FocusRequester,
    onType: (String) -> Unit,
    onDelete: () -> Unit,
    onSpace: () -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x2A0E1420))
            .border(BorderStroke(1.dp, Color(0x24FFFFFF)), RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "屏幕键盘",
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val spacing = 8.dp
            val containerWidth = maxWidth
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyboardRows.forEachIndexed { rowIndex, row ->
                    val keyWidth = ((containerWidth - spacing * (row.size - 1)) / row.size).coerceIn(42.dp, 58.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        row.forEachIndexed { keyIndex, key ->
                            KeyboardKey(
                                text = key,
                                width = keyWidth,
                                modifier = if (rowIndex == 0 && keyIndex == 0) Modifier.focusRequester(firstKeyFocus) else Modifier,
                                onClick = { onType(key.lowercase()) }
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KeyboardActionKey(text = "删除", modifier = Modifier.weight(1f), onClick = onDelete)
            KeyboardActionKey(text = "空格", modifier = Modifier.weight(1f), onClick = onSpace)
            KeyboardActionKey(text = "清空", modifier = Modifier.weight(1f), onClick = onClear)
            KeyboardActionKey(text = "搜索", modifier = Modifier.weight(1.2f), strong = true, onClick = onSearch)
        }
    }
}

@Composable
private fun SearchAssistPanel(
    modifier: Modifier,
    loading: Boolean,
    loadingSuggest: Boolean,
    history: List<String>,
    hotKeywords: List<String>,
    suggestKeywords: List<String>,
    onPick: (String) -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x2A0E1420))
            .border(BorderStroke(1.dp, Color(0x24FFFFFF)), RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "搜索辅助",
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold
        )

        AssistSection(
            title = "联想输入",
            words = if (loadingSuggest) listOf("联想中...") else suggestKeywords,
            disabled = loadingSuggest || suggestKeywords.isEmpty(),
            onPick = onPick
        )

        AssistSection(
            title = "历史记录",
            words = history.ifEmpty { listOf("暂无历史") },
            disabled = history.isEmpty(),
            onPick = onPick
        )

        AssistSection(
            title = "热搜榜",
            words = if (loading) listOf("加载中...") else hotKeywords,
            disabled = loading || hotKeywords.isEmpty(),
            onPick = onPick
        )
    }
}

@Composable
private fun AssistSection(
    title: String,
    words: List<String>,
    disabled: Boolean,
    onPick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = Color(0xFFD7E1F2),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(words) { word ->
                AssistChip(
                    text = word,
                    enabled = !disabled,
                    onClick = { onPick(word) }
                )
            }
        }
    }
}

@Composable
private fun SearchSystemInputOverlay(
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    val inputFocusRequester = remember { FocusRequester() }

    BackHandler(enabled = true) { onDismiss() }

    LaunchedEffect(Unit) {
        delay(60)
        runCatching { inputFocusRequester.requestFocus() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xAA000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(min = 680.dp, max = 980.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xE61A2230))
                .border(BorderStroke(1.dp, Color(0x3CFFFFFF)), RoundedCornerShape(24.dp))
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "系统输入",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "可直接输入中文/英文/数字/符号",
                color = Color(0xFFD0DBEE),
                fontSize = 13.sp
            )
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(inputFocusRequester),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 20.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                label = { Text("输入关键词", color = Color(0xFFADBBD4)) }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SearchChip(text = "取消") { onDismiss() }
                    SearchChip(text = "搜索") { onConfirm(value.trim()) }
                }
            }
        }
    }
}

@Composable
private fun SearchResultGrid(
    items: List<VideoCard>,
    searched: Boolean,
    loadingMore: Boolean,
    hasMore: Boolean,
    restoreFocusVideoId: String?,
    onRestoreFocusConsumed: () -> Unit,
    onReachEnd: () -> Unit,
    onOpen: (VideoCard) -> Unit
) {
    if (!searched) {
        Text(
            text = "输入关键词或拼音首字母后按搜索。",
            color = Color(0xFFC8D3E8),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 20.dp)
        )
        return
    }
    val normalized = items
        .filter { it.id.isNotBlank() && it.title.isNotBlank() }
        .distinctBy { it.id }
    if (normalized.isEmpty()) {
        Text(
            text = "没有找到内容，试试热搜词或缩短关键词。",
            color = Color(0xFFC8D3E8),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 20.dp)
        )
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val spacing = 14.dp
        val cardWidth = ((maxWidth - spacing * 3) / 4).coerceIn(150.dp, 320.dp)
        val rows = normalized.chunked(4)
        val totalItems = normalized.size
        val restoreFocusRequester = remember { FocusRequester() }
        val hasRestoreTarget = remember(normalized, restoreFocusVideoId) {
            !restoreFocusVideoId.isNullOrBlank() && normalized.any { it.id == restoreFocusVideoId }
        }

        LaunchedEffect(restoreFocusVideoId, hasRestoreTarget, normalized.size) {
            if (restoreFocusVideoId == null) return@LaunchedEffect
            delay(36)
            if (hasRestoreTarget) {
                runCatching { restoreFocusRequester.requestFocus() }
            }
            onRestoreFocusConsumed()
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            rows.forEachIndexed { rowIndex, rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    rowItems.forEachIndexed { itemIndex, item ->
                        val globalIndex = rowIndex * 4 + itemIndex
                        val nearEnd = hasMore && !loadingMore && totalItems > 0 && globalIndex >= (totalItems - 8).coerceAtLeast(0)
                        SearchGridCard(
                            item = item,
                            cardWidth = cardWidth,
                            focusRequester = if (!restoreFocusVideoId.isNullOrBlank() && item.id == restoreFocusVideoId) restoreFocusRequester else null,
                            onFocused = if (nearEnd) onReachEnd else null
                        ) { onOpen(item) }
                    }
                    repeat((4 - rowItems.size).coerceAtLeast(0)) {
                        Spacer(modifier = Modifier.width(cardWidth))
                    }
                }
            }
            if (loadingMore || hasMore) {
                Text(
                    text = if (loadingMore) "正在加载更多..." else "向下继续浏览可自动加载",
                    color = Color(0xFFC8D3E8),
                    fontSize = 12.sp
                )
            } else {
                Text(
                    text = "没有更多内容了",
                    color = Color(0xFF9EB1D1),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun SearchGridCard(
    item: VideoCard,
    cardWidth: androidx.compose.ui.unit.Dp,
    focusRequester: FocusRequester? = null,
    onFocused: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val border by animateColorAsState(if (focused) Color(0xFFE4ECFF) else Color(0x20FFFFFF), label = "searchPosterBorder")

    Column(
        modifier = Modifier
            .width(cardWidth)
            .widthIn(min = 150.dp)
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
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF131D30))
                .border(BorderStroke(if (focused) 2.dp else 1.dp, border), RoundedCornerShape(24.dp))
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
                    text = "${item.author.ifBlank { "未知UP主" }} · ${item.publishDate}",
                    color = Color(0xFFAFC0DD),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun KeyboardKey(text: String, width: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .width(width)
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (focused) Color(0x5AD8E5FF) else Color(0x20FFFFFF))
            .border(
                BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFE8EEFF) else Color(0x28FFFFFF)),
                RoundedCornerShape(12.dp)
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun KeyboardActionKey(text: String, modifier: Modifier = Modifier, strong: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    focused -> Color(0x5AD8E5FF)
                    strong -> Color(0x3CD8E5FF)
                    else -> Color(0x20FFFFFF)
                }
            )
            .border(
                BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFE8EEFF) else Color(0x28FFFFFF)),
                RoundedCornerShape(12.dp)
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AssistChip(text: String, enabled: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (focused) Color(0x5AD8E5FF) else Color(0x20FFFFFF))
            .border(
                BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFE8EEFF) else Color(0x28FFFFFF)),
                RoundedCornerShape(999.dp)
            )
            .focusable(enabled = enabled, interactionSource = interaction)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else Color(0xFF8EA0BF),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SearchChip(text: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "searchChipScale")

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(999.dp))
            .background(if (focused) Color(0x5CD8E5FF) else Color(0x22FFFFFF))
            .border(
                BorderStroke(if (focused) 2.dp else 1.dp, if (focused) Color(0xFFE8EEFF) else Color(0x27FFFFFF)),
                RoundedCornerShape(999.dp)
            )
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

private fun keyToChar(key: Key): String? {
    return when (key) {
        Key.A -> "a"
        Key.B -> "b"
        Key.C -> "c"
        Key.D -> "d"
        Key.E -> "e"
        Key.F -> "f"
        Key.G -> "g"
        Key.H -> "h"
        Key.I -> "i"
        Key.J -> "j"
        Key.K -> "k"
        Key.L -> "l"
        Key.M -> "m"
        Key.N -> "n"
        Key.O -> "o"
        Key.P -> "p"
        Key.Q -> "q"
        Key.R -> "r"
        Key.S -> "s"
        Key.T -> "t"
        Key.U -> "u"
        Key.V -> "v"
        Key.W -> "w"
        Key.X -> "x"
        Key.Y -> "y"
        Key.Z -> "z"
        Key.Zero -> "0"
        Key.One -> "1"
        Key.Two -> "2"
        Key.Three -> "3"
        Key.Four -> "4"
        Key.Five -> "5"
        Key.Six -> "6"
        Key.Seven -> "7"
        Key.Eight -> "8"
        Key.Nine -> "9"
        Key.Spacebar -> " "
        else -> null
    }
}

private fun navigateToHome(navController: NavController) {
    navController.navigate("home") {
        launchSingleTop = true
        popUpTo(navController.graph.startDestinationId) { inclusive = false }
        restoreState = true
    }
}
