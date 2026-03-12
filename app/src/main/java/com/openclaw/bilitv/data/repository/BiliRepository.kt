package com.openclaw.bilitv.data.repository

import android.content.Context
import com.openclaw.bilitv.data.api.BiliApiService
import com.openclaw.bilitv.data.api.BiliPassportApiService
import com.openclaw.bilitv.data.api.BiliSuggestApiService
import com.openclaw.bilitv.data.model.BiliResponse
import com.openclaw.bilitv.data.model.FeedItem
import com.openclaw.bilitv.data.model.DashStreamItem
import com.openclaw.bilitv.data.model.FavoriteMediaItem
import com.openclaw.bilitv.data.model.HistoryItem
import com.openclaw.bilitv.data.model.PlaybackInfo
import com.openclaw.bilitv.data.model.PlaybackQualityOption
import com.openclaw.bilitv.data.model.PlayUrlData
import com.openclaw.bilitv.data.model.QrCodePollData
import com.openclaw.bilitv.data.model.RankingItem
import com.openclaw.bilitv.data.model.RelatedVideoItem
import com.openclaw.bilitv.data.model.ReplyItem
import com.openclaw.bilitv.data.model.SearchResultItem
import com.openclaw.bilitv.data.model.SpaceVideoItem
import com.openclaw.bilitv.data.model.ToViewItem
import com.openclaw.bilitv.data.model.VideoComment
import com.openclaw.bilitv.model.VideoCard
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToLong

object BiliRepository {
    private const val REFERER = "https://www.bilibili.com"
    private const val USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    private const val PREFS_NAME = "bili_tv_auth"
    private const val KEY_COOKIES_JSON = "cookies_json"
    private val cookieStore = linkedMapOf<String, String>()
    private val cookieLock = Any()
    private var appContext: Context? = null
    private val aidCache = linkedMapOf<String, Long>()
    private val partitionSortOrder = listOf(
        1, 13, 167, 3, 129, 4, 36, 188, 234, 223, 160, 211, 217, 119, 155, 202, 5, 181, 177, 11, 23, 75, 76, 17, 138, 165
    )
    private val partitionNameByRid = mapOf(
        1 to "动画",
        3 to "音乐",
        4 to "游戏",
        5 to "娱乐",
        17 to "单机游戏",
        11 to "电视剧",
        13 to "番剧",
        23 to "电影",
        36 to "知识",
        75 to "动物综合",
        76 to "电影综合",
        119 to "鬼畜",
        129 to "舞蹈",
        138 to "搞笑",
        155 to "时尚",
        160 to "生活",
        165 to "广告",
        167 to "国创",
        177 to "纪录片",
        181 to "影视",
        188 to "科技",
        202 to "资讯",
        211 to "美食",
        217 to "动物圈",
        223 to "汽车",
        234 to "运动"
    )
    private val partitionCatalogDefaults = listOf(
        1, 3, 4, 5, 11, 13, 17, 23, 36, 75, 76, 119, 129, 138, 155, 160, 165, 167, 177, 181, 188, 202, 211, 217, 223, 234
    )
    private val partitionChildFallback = mapOf(
        11 to listOf(185, 187, 11),
        13 to listOf(152, 33, 32),
        23 to listOf(145, 146, 147, 83),
        167 to listOf(153, 168, 169),
        177 to listOf(178, 179, 180),
        75 to listOf(217, 75)
    )
    private val partitionKeywordFallbackByRid = mapOf(
        11 to "电视剧",
        13 to "番剧",
        23 to "电影",
        167 to "国创",
        177 to "纪录片"
    )

    data class QrLoginSession(
        val qrCodeKey: String,
        val loginUrl: String,
        val qrImageUrl: String
    )

    enum class QrPollState {
        WAITING_SCAN,
        SCANNED_WAIT_CONFIRM,
        SUCCESS,
        EXPIRED,
        UNKNOWN
    }

    data class QrPollResult(
        val state: QrPollState,
        val message: String
    )

    data class UserProfile(
        val name: String,
        val mid: String,
        val avatar: String,
        val level: Int,
        val coin: Double
    )

    data class PartitionDirectory(
        val rid: Int,
        val name: String,
        val count: Int
    )

    data class PlaybackPage(
        val cid: Long,
        val page: Int,
        val title: String,
        val durationSec: Long,
        val durationLabel: String
    )

    data class DanmakuSample(
        val timeMs: Long,
        val text: String
    )

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.NONE
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        // B 站接口容易对缺少 UA/Referer 的请求返回 412/-352
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .header("Referer", REFERER)
                .apply {
                    val cookieHeader = buildCookieHeader()
                    if (cookieHeader.isNotBlank()) {
                        header("Cookie", cookieHeader)
                    }
                }
                .build()
            val response = chain.proceed(request)
            response.headers("Set-Cookie").forEach(::saveCookieFromHeader)
            response
        }
        .addInterceptor(logging)
        .build()

    private val api = Retrofit.Builder()
        .baseUrl("https://api.bilibili.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(BiliApiService::class.java)

    private val passportApi = Retrofit.Builder()
        .baseUrl("https://passport.bilibili.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(BiliPassportApiService::class.java)

    private val suggestApi = Retrofit.Builder()
        .baseUrl("https://s.search.bilibili.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(BiliSuggestApiService::class.java)

    fun initialize(context: Context) {
        appContext = context.applicationContext
        restoreCookiesFromDisk()
    }

    fun isLoggedIn(): Boolean {
        return hasSessionCookie()
    }

    fun hasSessionCookie(): Boolean = synchronized(cookieLock) {
        !cookieStore["SESSDATA"].isNullOrBlank()
    }

    fun hasCsrfCookie(): Boolean = synchronized(cookieLock) {
        !cookieStore["bili_jct"].isNullOrBlank()
    }

    fun clearLoginSession() {
        synchronized(cookieLock) {
            cookieStore.clear()
        }
        persistCookiesToDisk()
    }

    suspend fun fetchCurrentUserProfile(): UserProfile? {
        val nav = api.getNav().requireData("用户信息")
        val navLoggedIn = nav.isLogin == true || nav.isLoginInt == 1
        if (!navLoggedIn && !isLoggedIn()) return null
        return UserProfile(
            name = cleanText(nav.uname).ifBlank { "B站用户" },
            mid = nav.mid?.toString().orEmpty(),
            avatar = normalizeUrl(upgradeImageUrl(nav.face.orEmpty())),
            level = nav.levelInfo?.currentLevel ?: 0,
            coin = nav.money ?: 0.0
        )
    }

    suspend fun createQrLoginSession(): QrLoginSession {
        val data = passportApi.generateQrCode().requireData("扫码登录")
        val loginUrl = data.url?.trim().orEmpty()
        val qrCodeKey = data.qrcodeKey?.trim().orEmpty()
        if (loginUrl.isBlank() || qrCodeKey.isBlank()) error("未获取到扫码登录二维码")

        val encoded = URLEncoder.encode(loginUrl, StandardCharsets.UTF_8.name())
        val qrImageUrl = "https://quickchart.io/qr?size=360&text=$encoded"
        return QrLoginSession(
            qrCodeKey = qrCodeKey,
            loginUrl = loginUrl,
            qrImageUrl = qrImageUrl
        )
    }

    suspend fun pollQrLogin(qrCodeKey: String): QrPollResult {
        val response = passportApi.pollQrCode(qrCodeKey)
        response.headers().values("Set-Cookie").forEach(::saveCookieFromHeader)
        val body = response.body() ?: error("扫码状态返回空数据")
        if (body.code != 0) {
            return QrPollResult(
                state = QrPollState.UNKNOWN,
                message = body.message.ifBlank { "扫码状态异常" }
            )
        }
        val data = body.data ?: error("扫码状态缺少 data")
        return mapQrPollResult(data)
    }

    suspend fun likeVideo(bvid: String): String {
        val aid = resolveAidByBvid(bvid)
        val csrf = requireCsrf()
        val response = api.setVideoLike(
            aid = aid,
            bvid = bvid,
            like = 1,
            csrf = csrf,
            csrfToken = csrf
        )
        if (response.code == 0) return "点赞成功"
        val msg = response.message.ifBlank { "点赞失败" }
        if (response.code in setOf(65004, 65006) || msg.containsAny("重复点赞", "已赞", "已点赞")) return "已点赞"
        if (response.code == -101 || msg.containsAny("请先登录", "csrf")) error("请先扫码登录")
        error("点赞失败(code=${response.code}): $msg")
    }

    suspend fun dislikeVideo(bvid: String): String {
        val aid = resolveAidByBvid(bvid)
        val csrf = requireCsrf()
        val response = api.setVideoLike(
            aid = aid,
            bvid = bvid,
            like = 2,
            csrf = csrf,
            csrfToken = csrf
        )
        if (response.code == 0) return "已取消点赞"
        val msg = response.message.ifBlank { "操作失败" }
        if (msg.containsAny("未点赞", "请勿重复操作", "已取消")) return "已取消点赞"
        if (response.code == -101 || msg.containsAny("请先登录", "csrf")) error("请先扫码登录")
        error("不喜欢操作失败(code=${response.code}): $msg")
    }

    suspend fun coinVideo(bvid: String): String {
        val aid = resolveAidByBvid(bvid)
        val csrf = requireCsrf()
        val response = api.addVideoCoin(
            aid = aid,
            bvid = bvid,
            multiply = 1,
            selectLike = 0,
            csrf = csrf,
            csrfToken = csrf
        )
        if (response.code == 0) return "投币成功"
        val msg = response.message.ifBlank { "投币失败" }
        if (response.code in setOf(34005, 34009) || msg.containsAny("已投币", "重复投币", "已经投过币")) return "已投币"
        if (msg.contains("硬币不足")) return "硬币不足"
        if (response.code == -101 || msg.containsAny("请先登录", "csrf")) error("请先扫码登录")
        error("投币失败(code=${response.code}): $msg")
    }

    suspend fun favoriteVideo(bvid: String): String {
        val aid = resolveAidByBvid(bvid)
        val csrf = requireCsrf()
        val nav = api.getNav().requireData("用户信息")
        val mid = nav.mid ?: error("未获取到登录账号信息")
        val createdFolderData = runCatching {
            api.getCreatedFavoriteFolders(upMid = mid, type = 2).requireData("收藏夹列表")
        }.getOrNull()
        val mediaId = createdFolderData?.list?.firstOrNull { it.id != null }?.id
            ?: runCatching {
                api.getFavoriteFolders(type = 2, rid = aid).requireData("收藏夹列表")
            }.getOrNull()?.list?.firstOrNull { it.id != null }?.id
            ?: error("未获取到收藏夹")
        val response = api.dealFavoriteResource(
            rid = aid,
            type = 2,
            addMediaIds = mediaId.toString(),
            delMediaIds = "",
            csrf = csrf,
            csrfToken = csrf
        )
        if (response.code == 0) return "收藏成功"
        val msg = response.message.ifBlank { "收藏失败" }
        if (response.code in setOf(11007, 11008) || msg.containsAny("已收藏", "重复收藏", "已在收藏夹")) return "已收藏"
        if (response.code == -101 || msg.containsAny("请先登录", "csrf")) error("请先扫码登录")
        error("收藏失败(code=${response.code}): $msg")
    }

    suspend fun followUpByVideo(bvid: String): String {
        val detail = api.getVideoDetail(bvid).requireData("视频详情").view ?: error("未获取到视频详情")
        val mid = detail.owner?.mid ?: error("未获取到 UP 主信息")
        val csrf = requireCsrf()
        val response = api.modifyRelation(
            targetMid = mid,
            action = 1,
            reSrc = 11,
            csrf = csrf,
            csrfToken = csrf
        )
        if (response.code == 0) return "关注成功"
        val msg = response.message.ifBlank { "关注失败" }
        if (response.code in setOf(22004, 22115) || msg.containsAny("已关注", "重复关注")) return "已关注"
        if (response.code == -101 || msg.containsAny("请先登录", "csrf")) error("请先扫码登录")
        error("关注失败(code=${response.code}): $msg")
    }

    suspend fun isFollowingUpByVideo(bvid: String): Boolean {
        if (!isLoggedIn()) return false
        val detail = api.getVideoDetail(bvid).requireData("视频详情").view ?: return false
        val mid = detail.owner?.mid ?: return false
        val response = api.getRelationStat(mid)
        if (response.code != 0) return false
        val attr = response.data?.attribute ?: 0
        return (attr and 2) == 2
    }

    suspend fun toggleFollowUpByVideo(bvid: String, follow: Boolean): String {
        val detail = api.getVideoDetail(bvid).requireData("视频详情").view ?: error("未获取到视频详情")
        val mid = detail.owner?.mid ?: error("未获取到 UP 主信息")
        val csrf = requireCsrf()
        val action = if (follow) 1 else 2
        val response = api.modifyRelation(
            targetMid = mid,
            action = action,
            reSrc = 11,
            csrf = csrf,
            csrfToken = csrf
        )
        if (response.code == 0) return if (follow) "已关注" else "已取消关注"
        val msg = response.message.ifBlank { if (follow) "关注失败" else "取消关注失败" }
        if (response.code == -101 || msg.containsAny("请先登录", "csrf")) error("请先扫码登录")
        if (msg.containsAny("已关注", "重复关注")) return "已关注"
        if (msg.containsAny("未关注", "已取消")) return "已取消关注"
        error("${if (follow) "关注" else "取消关注"}失败(code=${response.code}): $msg")
    }

    suspend fun fetchHomeVideos(pageSize: Int = 16, freshIndex: Int = 1): List<VideoCard> {
        val data = api.getHomeFeed(pageSize = pageSize, freshIndex = freshIndex).requireData("首页推荐")
        return data.items.mapNotNull { mapFeedItem(it, fallbackCategory = "推荐") }
    }

    suspend fun fetchHomeVideosByTabLabel(
        tabLabel: String,
        page: Int = 1,
        pageSize: Int = 24,
        refreshKey: Int = 0
    ): List<VideoCard> {
        val safeKey = refreshKey.coerceAtLeast(0)
        val normalizedPage = page.coerceAtLeast(1)
        val normalizedPageSize = pageSize.coerceIn(8, 60)
        val raw = when (tabLabel) {
            "推荐" -> fetchHomeVideos(
                pageSize = normalizedPageSize,
                freshIndex = (normalizedPage + safeKey).coerceAtLeast(1)
            )
            "动态" -> runCatching {
                fetchNewVideos(page = normalizedPage + safeKey, pageSize = normalizedPageSize)
            }.getOrElse {
                fetchPopularVideos(page = normalizedPage + safeKey, pageSize = normalizedPageSize)
            }
            "热门" -> fetchPopularVideos(page = normalizedPage + safeKey, pageSize = normalizedPageSize)
            "收藏" -> fetchFavoriteVideosSafe(page = normalizedPage, pageSize = normalizedPageSize)
            "全部分区" -> emptyList()
            "历史记录" -> fetchHistoryVideosSafe(page = normalizedPage, pageSize = normalizedPageSize)
            "稍后再看" -> if (normalizedPage == 1) fetchWatchLaterVideosSafe(limit = normalizedPageSize) else emptyList()
            "每周必看" -> fetchWeeklyMustWatchVideos(page = normalizedPage, pageSize = normalizedPageSize)
            "我的" -> fetchMyVideos(page = normalizedPage, pageSize = normalizedPageSize)
            else -> fetchHomeVideos(pageSize = normalizedPageSize, freshIndex = normalizedPage + safeKey)
        }
        return raw
            .filter { it.id.isNotBlank() && it.title.isNotBlank() }
            .distinctBy { it.id }
    }

    suspend fun fetchHotVideosByLabel(label: String, page: Int = 1, pageSize: Int = 24, refreshKey: Int = 0): List<VideoCard> {
        val safeKey = refreshKey.coerceAtLeast(0)
        val normalizedPage = page.coerceAtLeast(1)
        val normalizedPageSize = pageSize.coerceIn(8, 48)
        val raw = when (label) {
            "综合热门" -> fetchPopularVideos(page = normalizedPage + safeKey, pageSize = normalizedPageSize)
            "每周必看" -> runCatching {
                fetchWeeklyMustWatchVideos(page = normalizedPage, pageSize = normalizedPageSize)
            }.getOrElse {
                fetchRankingVideosSlice(regionId = 0, page = normalizedPage + safeKey, pageSize = normalizedPageSize)
            }
            "入站必刷" -> runCatching {
                fetchPreciousVideos(page = normalizedPage + safeKey, pageSize = normalizedPageSize)
            }.getOrElse {
                fetchNewVideos(page = normalizedPage + safeKey, pageSize = normalizedPageSize)
            }
            "排行榜" -> fetchGeneralRankingVideos(page = normalizedPage, pageSize = normalizedPageSize)
            "全站音乐榜" -> fetchMusicBoardVideos(page = normalizedPage, pageSize = normalizedPageSize)
            else -> fetchPopularVideos(page = normalizedPage + safeKey, pageSize = normalizedPageSize)
        }
        return raw
            .filter { it.id.isNotBlank() && it.title.isNotBlank() }
            .distinctBy { it.id }
    }

    private suspend fun fetchHistoryVideosSafe(page: Int, pageSize: Int): List<VideoCard> {
        if (!isLoggedIn()) return emptyList()
        return fetchHistoryVideos(page = page, pageSize = pageSize)
    }

    private suspend fun fetchWatchLaterVideosSafe(limit: Int): List<VideoCard> {
        if (!isLoggedIn()) return emptyList()
        return fetchWatchLaterVideos(limit = limit)
    }

    private suspend fun fetchFavoriteVideosSafe(page: Int, pageSize: Int): List<VideoCard> {
        if (!isLoggedIn()) return emptyList()
        return fetchFavoriteVideos(page = page, pageSize = pageSize)
    }

    suspend fun fetchRecommendedVideos(excludeBvid: String? = null, limit: Int = 8): List<VideoCard> {
        val fromPopular = runCatching { fetchPopularVideos(page = 1, pageSize = limit * 2) }.getOrDefault(emptyList())
        val pool = if (fromPopular.isNotEmpty()) fromPopular else fetchHomeVideos(pageSize = limit * 2)
        return pool
            .filter { it.id.isNotBlank() && it.id != excludeBvid }
            .distinctBy { it.id }
            .take(limit)
    }

    suspend fun fetchAllPartitionDirectories(): List<PartitionDirectory> {
        val data = api.getOnlineRegions().requireData("全部分区")
        val onlineCountByRid = data.regionCount.mapNotNull { (ridRaw, count) ->
            ridRaw.toIntOrNull()?.let { rid -> rid to count.coerceAtLeast(0) }
        }.toMap()
        val allRids = (partitionCatalogDefaults + partitionNameByRid.keys + onlineCountByRid.keys)
            .distinct()
            .sorted()
        val parsed = allRids.map { rid ->
            PartitionDirectory(
                rid = rid,
                name = partitionNameByRid[rid] ?: "分区 $rid",
                count = onlineCountByRid[rid] ?: 0
            )
        }
        val orderIndex = partitionSortOrder.withIndex().associate { it.value to it.index }
        return parsed.sortedWith(
            compareBy<PartitionDirectory> { orderIndex[it.rid] ?: Int.MAX_VALUE }
                .thenByDescending { it.count }
                .thenBy { it.rid }
        )
    }

    suspend fun fetchPartitionVideosByRid(rid: Int, page: Int = 1, limit: Int = 36): List<VideoCard> {
        val normalizedRid = rid.coerceAtLeast(0)
        val normalizedPage = page.coerceAtLeast(1)
        val normalizedLimit = limit.coerceIn(8, 60)
        val byNewList = runCatching {
            fetchNewVideosByRidChunked(rid = normalizedRid, page = normalizedPage, pageSize = normalizedLimit)
        }.getOrDefault(emptyList())
        if (byNewList.isNotEmpty()) {
            return byNewList
                .filter { it.id.isNotBlank() && it.title.isNotBlank() }
                .distinctBy { it.id }
                .take(normalizedLimit)
        }

        val childMerged = partitionChildFallback[normalizedRid]
            .orEmpty()
            .flatMap { childRid ->
                runCatching {
                    fetchNewVideosByRidChunked(
                        rid = childRid,
                        page = normalizedPage,
                        pageSize = (normalizedLimit / 2).coerceAtLeast(12)
                    )
                }.getOrDefault(emptyList())
            }
            .filter { it.id.isNotBlank() && it.title.isNotBlank() }
            .distinctBy { it.id }
        if (childMerged.isNotEmpty()) {
            return childMerged.take(normalizedLimit)
        }

        val byKeyword = partitionKeywordFallbackByRid[normalizedRid]?.let { keyword ->
            runCatching {
                fetchPartitionVideosByKeyword(keyword = keyword, page = normalizedPage, pageSize = normalizedLimit)
            }.getOrDefault(emptyList())
        }.orEmpty()
        if (byKeyword.isNotEmpty()) {
            return byKeyword
                .filter { it.id.isNotBlank() && it.title.isNotBlank() }
                .distinctBy { it.id }
                .take(normalizedLimit)
        }

        val byRanking = runCatching {
            fetchRankingVideosSlice(regionId = normalizedRid, page = normalizedPage, pageSize = normalizedLimit)
        }.getOrDefault(emptyList())
        if (byRanking.isNotEmpty()) {
            return byRanking
                .filter { it.id.isNotBlank() && it.title.isNotBlank() }
                .distinctBy { it.id }
                .take(normalizedLimit)
        }
        return emptyList()
    }

    suspend fun fetchRelatedVideos(bvid: String, limit: Int = 8): List<VideoCard> {
        val detailData = api.getVideoDetail(bvid).requireData("视频详情")
        val related = detailData.related
            .mapNotNull(::mapRelatedItem)
            .filter { it.id.isNotBlank() && it.id != bvid }
            .distinctBy { it.id }
            .take(limit)
        return if (related.isNotEmpty()) {
            related
        } else {
            fetchRecommendedVideos(excludeBvid = bvid, limit = limit)
        }
    }

    suspend fun searchVideos(keyword: String, page: Int = 1, pageSize: Int = 24): List<VideoCard> {
        val response = api.searchVideos(
            keyword = keyword,
            page = page.coerceAtLeast(1),
            pageSize = pageSize.coerceIn(8, 60)
        )
        if (response.code != 0) error("搜索接口异常: ${response.code} ${response.message}")
        return response.data?.result.orEmpty()
            .map(::mapSearchItem)
            .filter { it.id.isNotBlank() && it.title.isNotBlank() }
    }

    suspend fun fetchSearchSuggestions(term: String, limit: Int = 12): List<String> {
        val query = term.trim()
        if (query.isBlank()) return emptyList()
        val response = suggestApi.getSearchSuggest(query)
        if (response.code != 0) error("联想接口异常: ${response.code}")
        val raw = response.result.values.flatten()
            .mapNotNull { item ->
                cleanText(item.value).ifBlank { cleanText(item.term) }.takeIf { it.isNotBlank() }
            }
        return raw.distinct().take(limit)
    }

    suspend fun fetchSearchDefaultKeyword(): String {
        val data = api.getSearchDefaultKeyword().requireData("搜索默认词")
        return cleanText(data.showName).ifBlank { cleanText(data.name) }
    }

    suspend fun fetchTrendingKeywords(limit: Int = 10): List<String> {
        val data = api.getSearchTrendingKeywords(limit = limit).requireData("搜索热榜")
        return data.trending?.list.orEmpty()
            .mapNotNull { item ->
                cleanText(item.showName).ifBlank { cleanText(item.keyword) }.takeIf { it.isNotBlank() }
            }
            .distinct()
            .take(limit)
    }

    suspend fun fetchVideoDetail(bvid: String): VideoCard? {
        val response = api.getVideoDetail(bvid)
        if (response.code != 0) error("视频详情接口异常: ${response.code} ${response.message}")
        val view = response.data?.view ?: return null
        val aid = view.aid
        if (aid != null && bvid.isNotBlank()) {
            aidCache[bvid] = aid
        }
        val resolvedFans = view.owner?.mid?.let { mid ->
            runCatching { api.getRelationStat(mid) }
                .getOrNull()
                ?.data
                ?.follower
        } ?: view.owner?.fans
        return VideoCard(
            id = view.bvid.orEmpty(),
            title = cleanText(view.title),
            author = cleanText(view.owner?.name).ifBlank { "未知UP主" },
            cover = normalizeUrl(upgradeImageUrl(view.pic.orEmpty())),
            duration = formatSeconds(view.duration ?: 0),
            description = cleanText(view.desc).ifBlank { "暂无简介" },
            category = cleanText(view.tname).ifBlank { "视频" },
            publishDate = formatDate(view.pubDate),
            avatar = normalizeUrl(upgradeImageUrl(view.owner?.face.orEmpty())),
            viewCount = formatCountText(view.stat?.view, "观看"),
            danmakuCount = formatCountText(view.stat?.danmaku, "弹幕"),
            fansCount = formatCountText(resolvedFans, "粉丝"),
            likeCount = formatCountText(view.stat?.like, "点赞"),
            coinCount = formatCountText(view.stat?.coin, "投币")
        )
    }

    suspend fun reportWatchProgress(
        bvid: String,
        cid: Long? = null,
        progressMs: Long = 0L
    ): Boolean {
        if (!hasSessionCookie()) return false
        val csrf = runCatching { requireCsrf() }.getOrElse { return false }
        val detail = api.getVideoDetail(bvid).requireData("视频详情").view ?: return false
        val aid = detail.aid ?: resolveAidByBvid(bvid)
        val targetCid = cid
            ?: detail.cid
            ?: runCatching { api.getPageList(bvid = bvid).requireData("分P").firstOrNull()?.cid }.getOrNull()
            ?: detail.pages.firstOrNull()?.cid
            ?: return false
        val progressSeconds = (progressMs / 1000L).coerceAtLeast(1L)
        val attempts = listOf<suspend () -> BiliResponse<Any>>(
            {
                api.reportHistoryProgress(
                    aid = aid,
                    cid = targetCid,
                    progress = progressSeconds,
                    platform = "android",
                    csrf = csrf,
                    csrfToken = csrf
                )
            },
            {
                api.reportHistoryProgress(
                    aid = aid,
                    cid = targetCid,
                    progress = progressSeconds,
                    platform = "web",
                    csrf = csrf,
                    csrfToken = csrf
                )
            },
            {
                api.reportHistoryProgressSimple(
                    aid = aid,
                    cid = targetCid,
                    progress = progressSeconds,
                    platform = "android",
                    csrf = csrf
                )
            },
            {
                api.reportHistoryProgressSimple(
                    aid = aid,
                    cid = targetCid,
                    progress = null,
                    platform = "android",
                    csrf = csrf
                )
            },
            {
                api.reportHistoryProgressSimple(
                    aid = aid,
                    cid = targetCid,
                    progress = null,
                    platform = "web",
                    csrf = csrf
                )
            }
        )
        attempts.forEach { request ->
            val response = runCatching { request() }.getOrNull() ?: return@forEach
            if (response.code == 0) return true
        }
        return false
    }

    private suspend fun fetchPartitionVideosByKeyword(keyword: String, page: Int, pageSize: Int): List<VideoCard> {
        val query = keyword.trim()
        if (query.isBlank()) return emptyList()
        return searchVideos(
            keyword = query,
            page = page.coerceAtLeast(1),
            pageSize = pageSize.coerceIn(8, 60)
        )
            .filter { it.id.isNotBlank() && it.title.isNotBlank() }
            .distinctBy { it.id }
    }

    private suspend fun fetchNewVideosByRidChunked(rid: Int, page: Int = 1, pageSize: Int = 20): List<VideoCard> {
        val normalizedPage = page.coerceAtLeast(1)
        val normalizedSize = pageSize.coerceIn(8, 60)
        val requestSize = normalizedSize.coerceAtMost(24)
        val requestCount = ((normalizedSize + requestSize - 1) / requestSize).coerceAtLeast(1)
        val startPage = ((normalizedPage - 1) * requestCount) + 1
        val merged = mutableListOf<VideoCard>()
        for (offset in 0 until requestCount) {
            val currentPage = startPage + offset
            val chunk = runCatching {
                fetchNewVideosByRid(
                    rid = rid,
                    page = currentPage,
                    pageSize = requestSize
                )
            }.getOrDefault(emptyList())
            if (chunk.isEmpty()) {
                if (offset == 0) return emptyList()
                break
            }
            chunk.forEach { card ->
                if (card.id.isNotBlank() && merged.none { it.id == card.id }) {
                    merged += card
                }
            }
            if (chunk.size < requestSize) break
            if (merged.size >= normalizedSize) break
        }
        return merged.take(normalizedSize)
    }

    suspend fun fetchPlaybackPages(bvid: String): List<PlaybackPage> {
        val pages = runCatching {
            api.getPageList(bvid = bvid).requireData("分P")
        }.getOrDefault(emptyList())
        if (pages.isNotEmpty()) {
            return pages.mapIndexedNotNull { index, item ->
                val cid = item.cid ?: return@mapIndexedNotNull null
                val page = item.page ?: (index + 1)
                val title = cleanText(item.part).ifBlank { "第${page}P" }
                val durationSec = item.duration ?: 0L
                PlaybackPage(
                    cid = cid,
                    page = page,
                    title = title,
                    durationSec = durationSec,
                    durationLabel = formatSeconds(durationSec)
                )
            }
        }
        val detailPages = api.getVideoDetail(bvid).requireData("视频详情").view?.pages.orEmpty()
        return detailPages.mapIndexedNotNull { index, item ->
            val cid = item.cid ?: return@mapIndexedNotNull null
            val page = item.page ?: (index + 1)
            val title = cleanText(item.part).ifBlank { "第${page}P" }
            val durationSec = item.duration ?: 0L
            PlaybackPage(
                cid = cid,
                page = page,
                title = title,
                durationSec = durationSec,
                durationLabel = formatSeconds(durationSec)
            )
        }
    }

    suspend fun fetchPlaybackInfo(
        bvid: String,
        preferredQn: Int? = null,
        targetCid: Long? = null,
        partTitle: String? = null,
        partDurationSec: Long? = null
    ): PlaybackInfo {
        val detail = api.getVideoDetail(bvid).requireData("视频详情").view ?: error("未获取到视频详情")
        val cid = targetCid
            ?: detail.cid
            ?: api.getPageList(bvid = bvid).requireData("分P").firstOrNull()?.cid
            ?: detail.pages.firstOrNull()?.cid
            ?: error("未获取到 cid")
        val baseTitle = cleanText(detail.title)
        val parsedPart = cleanText(partTitle)
        val title = if (parsedPart.isBlank()) baseTitle else "$baseTitle  ·  $parsedPart"
        val subtitleDuration = partDurationSec ?: detail.duration ?: 0L
        val subtitle = "${cleanText(detail.owner?.name).ifBlank { "未知UP主" }} · ${formatSeconds(subtitleDuration)}"
        val cover = normalizeUrl(upgradeImageUrl(detail.pic.orEmpty()))
        val qnCandidates = (listOfNotNull(preferredQn) + listOf(80, 64, 32, 16)).distinct()
        val errors = mutableListOf<String>()

        val navKeys = queryWbiKeys()

        if (navKeys != null) {
            val (imgKey, subKey) = navKeys
            for (qn in qnCandidates) {
                val built = runCatching {
                    val playData = requestWbiPlayData(bvid = bvid, cid = cid, qn = qn, imgKey = imgKey, subKey = subKey)
                    buildPlaybackInfoFromPlayData(
                        playData = playData,
                        expectedQn = qn,
                        title = title,
                        subtitle = subtitle,
                        cover = cover
                    )
                }
                built.onSuccess { return it }
                built.onFailure { errors += "WBI(qn=$qn): ${it.message}" }
            }
        } else {
            errors += "WBI签名参数获取失败"
        }

        for (qn in qnCandidates) {
            val built = runCatching {
                val playData = api.getPlayUrlLegacy(
                    bvid = bvid,
                    cid = cid,
                    qn = qn,
                    fnval = 0,
                    fnver = 0,
                    fourk = 0
                ).requireData("播放地址")
                buildPlaybackInfoFromPlayData(
                    playData = playData,
                    expectedQn = qn,
                    title = title,
                    subtitle = subtitle,
                    cover = cover
                )
            }
            built.onSuccess { return it }
            built.onFailure { errors += "Legacy(qn=$qn): ${it.message}" }
        }

        error(errors.firstOrNull() ?: "未获取到可播放地址")
    }

    suspend fun fetchVideoComments(bvid: String, page: Int = 1, pageSize: Int = 18): List<VideoComment> {
        val detail = api.getVideoDetail(bvid).requireData("视频详情").view ?: error("未获取到视频详情")
        val aid = detail.aid ?: error("未获取到 aid")
        val replies = api.getVideoReplies(
            oid = aid,
            page = page,
            pageSize = pageSize,
            sort = 2
        ).requireData("评论")

        return replies.replies
            .map(::mapReplyItem)
            .filter { it.content.isNotBlank() }
    }

    suspend fun fetchDanmakuSamples(
        bvid: String,
        cid: Long? = null,
        limit: Int = 120
    ): List<DanmakuSample> = withContext(Dispatchers.IO) {
        val detail = api.getVideoDetail(bvid).requireData("视频详情").view ?: return@withContext emptyList()
        val targetCid = cid
            ?: detail.cid
            ?: runCatching { api.getPageList(bvid = bvid).requireData("分P").firstOrNull()?.cid }.getOrNull()
            ?: detail.pages.firstOrNull()?.cid
            ?: return@withContext emptyList()
        val merged = linkedMapOf<String, DanmakuSample>()

        // 新版弹幕主链路：分段 protobuf（开源项目普遍使用这条链路）
        var emptySegCount = 0
        for (segment in 1..12) {
            val segUrl = "https://api.bilibili.com/x/v2/dm/web/seg.so?type=1&oid=$targetCid&segment_index=$segment"
            val samples = runCatching {
                val payload = requestBytes(segUrl)
                parseDanmakuProtobuf(payload, limit)
            }.getOrDefault(emptyList())
            if (samples.isEmpty()) {
                emptySegCount += 1
                if (segment >= 2 && emptySegCount >= 2) break
                continue
            }
            emptySegCount = 0
            samples.forEach { sample ->
                merged["${sample.timeMs}:${sample.text}"] = sample
            }
            if (merged.size >= limit) break
        }

        // 旧链路兜底：xml / list.so
        if (merged.isEmpty()) {
            val fallbacks = listOf(
                "https://comment.bilibili.com/$targetCid.xml",
                "https://api.bilibili.com/x/v1/dm/list.so?oid=$targetCid"
            )
            fallbacks.forEach { url ->
                val payload = runCatching { requestBytes(url) }.getOrNull() ?: return@forEach
                val protobufParsed = parseDanmakuProtobuf(payload, limit)
                if (protobufParsed.isNotEmpty()) {
                    protobufParsed.forEach { sample ->
                        merged["${sample.timeMs}:${sample.text}"] = sample
                    }
                    return@forEach
                }
                val body = runCatching { payload.toString(Charsets.UTF_8) }.getOrDefault("")
                parseDanmakuXml(body, limit).forEach { sample ->
                    merged["${sample.timeMs}:${sample.text}"] = sample
                }
            }
        }

        merged.values
            .sortedBy { it.timeMs }
            .take(limit)
            .toList()
    }

    private fun requestBytes(url: String): ByteArray {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Referer", REFERER)
            .header("Accept-Encoding", "identity")
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use byteArrayOf()
            response.body?.bytes() ?: byteArrayOf()
        }
    }

    private fun parseDanmakuProtobuf(payload: ByteArray, limit: Int): List<DanmakuSample> {
        if (payload.isEmpty()) return emptyList()
        val result = mutableListOf<DanmakuSample>()
        var cursor = 0
        while (cursor < payload.size && result.size < limit) {
            val tagRead = readVarint(payload, cursor) ?: break
            cursor = tagRead.next
            val field = (tagRead.value ushr 3).toInt()
            val wireType = (tagRead.value and 0x07).toInt()

            if (field == 1 && wireType == 2) {
                val lenRead = readVarint(payload, cursor) ?: break
                cursor = lenRead.next
                val length = lenRead.value.toInt().coerceAtLeast(0)
                val end = (cursor + length).coerceAtMost(payload.size)
                parseDanmakuElem(payload, cursor, end)?.let(result::add)
                cursor = end
            } else {
                cursor = skipProtoField(payload, cursor, wireType) ?: break
            }
        }
        return result
            .filter { it.text.isNotBlank() }
            .distinctBy { "${it.timeMs}:${it.text}" }
            .sortedBy { it.timeMs }
            .take(limit)
    }

    private fun parseDanmakuElem(bytes: ByteArray, start: Int, end: Int): DanmakuSample? {
        var cursor = start
        var progressMs = 0L
        var content = ""

        while (cursor < end) {
            val tagRead = readVarint(bytes, cursor) ?: break
            cursor = tagRead.next
            val field = (tagRead.value ushr 3).toInt()
            val wireType = (tagRead.value and 0x07).toInt()

            when {
                field == 2 && wireType == 0 -> {
                    val valueRead = readVarint(bytes, cursor) ?: break
                    progressMs = valueRead.value.coerceAtLeast(0L)
                    cursor = valueRead.next
                }
                field == 7 && wireType == 2 -> {
                    val lenRead = readVarint(bytes, cursor) ?: break
                    cursor = lenRead.next
                    val length = lenRead.value.toInt().coerceAtLeast(0)
                    val textEnd = (cursor + length).coerceAtMost(end)
                    content = runCatching {
                        bytes.copyOfRange(cursor, textEnd).toString(Charsets.UTF_8)
                    }.getOrDefault("")
                    cursor = textEnd
                }
                else -> {
                    cursor = skipProtoField(bytes, cursor, wireType) ?: break
                }
            }
        }

        val normalized = decodeXmlText(content)
        if (normalized.isBlank()) return null
        return DanmakuSample(timeMs = progressMs, text = normalized)
    }

    private fun readVarint(bytes: ByteArray, start: Int): VarintReadResult? {
        var shift = 0
        var value = 0L
        var index = start
        while (index < bytes.size && shift <= 63) {
            val b = bytes[index].toInt() and 0xFF
            value = value or (((b and 0x7F).toLong()) shl shift)
            index += 1
            if ((b and 0x80) == 0) {
                return VarintReadResult(value = value, next = index)
            }
            shift += 7
        }
        return null
    }

    private fun skipProtoField(bytes: ByteArray, cursor: Int, wireType: Int): Int? {
        return when (wireType) {
            0 -> readVarint(bytes, cursor)?.next
            1 -> (cursor + 8).takeIf { it <= bytes.size }
            2 -> {
                val lenRead = readVarint(bytes, cursor) ?: return null
                val end = lenRead.next + lenRead.value.toInt().coerceAtLeast(0)
                end.coerceAtMost(bytes.size)
            }
            5 -> (cursor + 4).takeIf { it <= bytes.size }
            else -> null
        }
    }

    private fun parseDanmakuXml(xml: String, limit: Int): List<DanmakuSample> {
        val matcher = Regex("<d\\s+p=\"([^\"]+)\">(.*?)</d>")
        return matcher.findAll(xml)
            .mapNotNull { match ->
                val pMeta = match.groupValues.getOrNull(1).orEmpty()
                val seconds = pMeta.substringBefore(',').toDoubleOrNull() ?: return@mapNotNull null
                val text = decodeXmlText(match.groupValues.getOrNull(2).orEmpty())
                if (text.isBlank()) return@mapNotNull null
                DanmakuSample(
                    timeMs = (seconds * 1000.0).roundToLong().coerceAtLeast(0L),
                    text = text
                )
            }
            .distinctBy { (it.timeMs / 200L) to it.text }
            .sortedBy { it.timeMs }
            .take(limit)
            .toList()
    }

    private data class VarintReadResult(
        val value: Long,
        val next: Int
    )

    private suspend fun fetchHistoryVideos(page: Int = 1, pageSize: Int = 20): List<VideoCard> {
        if (!hasSessionCookie()) error("历史记录需要先扫码登录")
        val normalizedPage = page.coerceAtLeast(1)
        val normalizedSize = pageSize.coerceIn(8, 60)

        val cursorResult = runCatching {
            fetchHistoryVideosByCursor(page = normalizedPage, pageSize = normalizedSize)
        }
        if (cursorResult.isSuccess) {
            return cursorResult.getOrDefault(emptyList())
        }

        val legacyResponse = api.getWatchHistory(page = normalizedPage, pageSize = normalizedSize)
        if (legacyResponse.code != 0) {
            error(
                "历史记录请求失败: ${legacyResponse.message.ifBlank { "unknown" }}; " +
                    "cursor=${cursorResult.exceptionOrNull()?.message.orEmpty()}"
            )
        }
        val data = legacyResponse.data ?: return emptyList()
        return data.list.mapNotNull(::mapHistoryItem)
    }

    private suspend fun fetchHistoryVideosByCursor(page: Int, pageSize: Int): List<VideoCard> {
        var max = 0L
        var viewAt = 0L
        var currentPage = 1
        while (currentPage <= page) {
            val response = api.getWatchHistoryCursor(
                pageSize = pageSize,
                max = max,
                viewAt = viewAt,
                business = "archive"
            )
            if (response.code != 0) {
                error("cursor接口失败: code=${response.code}, message=${response.message}")
            }
            val data = response.data ?: return emptyList()
            val list = data.list
            if (currentPage == page) {
                return list.mapNotNull(::mapHistoryItem)
            }
            if (list.isEmpty()) return emptyList()
            val nextMax = data.cursor?.max
            val nextViewAt = data.cursor?.viewAt
            if (nextMax == null || nextViewAt == null) return emptyList()
            max = nextMax
            viewAt = nextViewAt
            currentPage += 1
        }
        return emptyList()
    }

    private suspend fun fetchWatchLaterVideos(limit: Int = 20): List<VideoCard> {
        if (!hasSessionCookie()) error("稍后再看需要先扫码登录")
        val response = api.getWatchLaterList()
        if (response.code != 0) error("稍后再看请求失败: ${response.message}")
        val data = response.data ?: return emptyList()
        return data.list
            .mapNotNull(::mapToViewItem)
            .distinctBy { it.id }
            .take(limit)
    }

    private suspend fun fetchFavoriteVideos(page: Int = 1, pageSize: Int = 20): List<VideoCard> {
        if (!hasSessionCookie()) error("收藏需要先扫码登录")
        val nav = api.getNav().requireData("用户信息")
        val mid = nav.mid ?: error("未获取到登录账号信息")
        val folderResponse = api.getCreatedFavoriteFolders(upMid = mid, type = 2)
        if (folderResponse.code != 0) error("收藏夹列表请求失败: ${folderResponse.message}")
        val folders = folderResponse.data?.list.orEmpty()
        val folderId = folders.firstOrNull { it.id != null }?.id ?: error("暂无收藏内容")
        val resourceResponse = api.getFavoriteFolderResources(
            mediaId = folderId,
            page = page,
            pageSize = pageSize
        )
        if (resourceResponse.code != 0) error("收藏内容请求失败: ${resourceResponse.message}")
        val resources = resourceResponse.data ?: return emptyList()
        return resources.medias.mapNotNull(::mapFavoriteMedia)
    }

    private suspend fun fetchMyVideos(page: Int = 1, pageSize: Int = 20): List<VideoCard> {
        if (!hasSessionCookie()) error("我的视频需要先扫码登录")
        val nav = api.getNav().requireData("用户信息")
        val mid = nav.mid ?: error("未获取到登录账号信息")
        val (imgKey, subKey) = queryWbiKeys() ?: error("我的视频签名参数获取失败")
        val signedParams = BiliWbi.sign(
            params = mapOf(
                "mid" to mid,
                "pn" to page,
                "ps" to pageSize,
                "order" to "pubdate"
            ),
            imgKey = imgKey,
            subKey = subKey
        )
        val data = api.getSpaceVideos(signedParams).requireData("我的视频")
        return data.list?.vlist.orEmpty().mapNotNull(::mapSpaceVideo)
    }

    private suspend fun fetchPopularVideos(page: Int = 1, pageSize: Int = 20): List<VideoCard> {
        val data = api.getPopularVideos(page = page, pageSize = pageSize).requireData("热门")
        return data.list.mapNotNull { mapFeedItem(it, fallbackCategory = "热门") }
    }

    private suspend fun fetchPreciousVideos(page: Int = 1, pageSize: Int = 20): List<VideoCard> {
        val data = api.getPopularPreciousVideos(page = 1, pageSize = 100).requireData("入站必刷")
        val mapped = data.list.mapNotNull { mapFeedItem(it, fallbackCategory = "入站必刷") }
        val start = ((page.coerceAtLeast(1) - 1) * pageSize).coerceAtLeast(0)
        if (start >= mapped.size) return emptyList()
        return mapped.drop(start).take(pageSize)
    }

    private suspend fun fetchWeeklyMustWatchVideos(page: Int = 1, pageSize: Int = 20): List<VideoCard> {
        val listData = api.getPopularSeriesList().requireData("每周必看")
        val series = listData.list
        if (series.isEmpty()) return emptyList()
        val target = series.getOrNull(page.coerceAtLeast(1) - 1) ?: return emptyList()
        val number = target.number ?: return emptyList()

        val signedParams = queryWbiKeys()?.let { (imgKey, subKey) ->
            BiliWbi.sign(
                params = mapOf("number" to number),
                imgKey = imgKey,
                subKey = subKey
            )
        } ?: mapOf("number" to number.toString())

        val oneData = runCatching {
            api.getPopularSeriesOne(signedParams).requireData("每周必看")
        }.getOrElse {
            val fallbackParams = mapOf("number" to number.toString())
            api.getPopularSeriesOne(fallbackParams).requireData("每周必看")
        }
        return oneData.list
            .mapNotNull { mapFeedItem(it, fallbackCategory = "每周必看") }
            .take(pageSize)
    }

    private suspend fun fetchNewVideos(page: Int = 1, pageSize: Int = 20): List<VideoCard> {
        val data = api.getNewVideos(page = page, pageSize = pageSize).requireData("新作")
        return data.archives.mapNotNull { mapFeedItem(it, fallbackCategory = "动态") }
    }

    private suspend fun fetchNewVideosByRid(rid: Int, page: Int = 1, pageSize: Int = 20): List<VideoCard> {
        val data = api.getNewVideos(
            rid = rid,
            page = page.coerceAtLeast(1),
            pageSize = pageSize.coerceIn(8, 60)
        ).requireData("分区")
        return data.archives
            .mapNotNull { mapFeedItem(it, fallbackCategory = partitionNameByRid[rid] ?: "分区") }
            .distinctBy { it.id }
    }

    private suspend fun fetchRankingVideosSlice(regionId: Int, page: Int, pageSize: Int): List<VideoCard> {
        val normalizedPage = page.coerceAtLeast(1)
        val normalizedPageSize = pageSize.coerceIn(8, 60)
        val ranked = runCatching {
            fetchRankingVideos(day = 3, limit = 200, regionId = regionId)
        }.getOrDefault(emptyList())

        if (ranked.isNotEmpty()) {
            val start = (normalizedPage - 1) * normalizedPageSize
            if (start < ranked.size) {
                return ranked.drop(start).take(normalizedPageSize)
            }
            val fallbackPage = ((start - ranked.size) / normalizedPageSize) + 1
            return fetchNewVideosByRidChunked(
                rid = regionId,
                page = fallbackPage,
                pageSize = normalizedPageSize
            ).take(normalizedPageSize)
        }

        return fetchNewVideosByRidChunked(
            rid = regionId,
            page = normalizedPage,
            pageSize = normalizedPageSize
        ).take(normalizedPageSize)
    }

    private suspend fun fetchGeneralRankingVideos(page: Int, pageSize: Int): List<VideoCard> {
        val rankingRids = listOf(0, 1, 3, 4, 5, 36, 119, 129, 160, 188, 217, 223, 234)
        val normalizedPage = page.coerceAtLeast(1)
        val boardIndex = (normalizedPage - 1) % rankingRids.size
        val boardRid = rankingRids[boardIndex]
        val boardPage = ((normalizedPage - 1) / rankingRids.size) + 1
        return fetchRankingVideosSlice(
            regionId = boardRid,
            page = boardPage,
            pageSize = pageSize
        )
    }

    private suspend fun fetchMusicBoardVideos(page: Int, pageSize: Int): List<VideoCard> {
        return fetchRankingVideosSlice(
            regionId = 3,
            page = page,
            pageSize = pageSize
        )
    }

    private suspend fun fetchRankingVideos(day: Int = 3, limit: Int = 20, regionId: Int = 0): List<VideoCard> {
        val data = api.getRankingVideos(regionId = regionId, day = day).requireData("排行榜")
        return data.list
            .mapNotNull(::mapRankingItem)
            .distinctBy { it.id }
            .take(limit)
    }

    private fun mapFeedItem(item: FeedItem, fallbackCategory: String): VideoCard? {
        val bvid = item.bvid?.takeIf { it.isNotBlank() } ?: return null
        return VideoCard(
            id = bvid,
            title = cleanText(item.title).ifBlank { "未命名视频" },
            author = cleanText(item.owner?.name ?: item.args?.upName).ifBlank { "未知UP主" },
            cover = normalizeUrl(upgradeImageUrl(item.cover ?: item.coverAlt ?: "")),
            duration = feedDurationText(item),
            description = cleanText(item.desc ?: item.reason?.content).ifBlank { "暂无简介" },
            category = cleanText(item.gotoField).ifBlank { fallbackCategory },
            publishDate = formatDate(item.pubDate),
            avatar = normalizeUrl(upgradeImageUrl(item.owner?.face.orEmpty())),
            viewCount = formatCountText(item.stat?.view, "观看"),
            danmakuCount = formatCountText(item.stat?.danmaku, "弹幕"),
            likeCount = formatCountText(item.stat?.like, "点赞"),
            coinCount = formatCountText(item.stat?.coin, "投币"),
            fansCount = "--粉丝"
        )
    }

    private fun mapRankingItem(item: RankingItem): VideoCard? {
        val bvid = item.bvid?.takeIf { it.isNotBlank() } ?: return null
        return VideoCard(
            id = bvid,
            title = cleanText(item.title).ifBlank { "未命名视频" },
            author = cleanText(item.author).ifBlank { "未知UP主" },
            cover = normalizeUrl(upgradeImageUrl(item.pic.orEmpty())),
            duration = item.duration?.trim().takeUnless { it.isNullOrBlank() } ?: "--:--",
            description = cleanText(item.desc).ifBlank { "排行榜视频" },
            category = cleanText(item.tname).ifBlank { "排行" },
            publishDate = formatDateOrRecent(item.pubDate),
            viewCount = formatCountFromRaw(item.play, "观看"),
            danmakuCount = formatCountFromRaw(item.videoReview, "弹幕"),
            likeCount = "--点赞",
            coinCount = formatCountFromRaw(item.coins, "投币"),
            fansCount = "--粉丝"
        )
    }

    private fun mapRelatedItem(item: RelatedVideoItem): VideoCard? {
        val bvid = item.bvid?.takeIf { it.isNotBlank() } ?: return null
        return VideoCard(
            id = bvid,
            title = cleanText(item.title).ifBlank { "未命名视频" },
            author = cleanText(item.owner?.name).ifBlank { "未知UP主" },
            cover = normalizeUrl(upgradeImageUrl(item.pic.orEmpty())),
            duration = formatSeconds(item.duration ?: 0),
            description = cleanText(item.desc).ifBlank { "相关推荐" },
            category = cleanText(item.tname).ifBlank { "相关推荐" },
            publishDate = formatDate(item.pubDate),
            avatar = normalizeUrl(upgradeImageUrl(item.owner?.face.orEmpty())),
            viewCount = formatCountText(item.stat?.view, "观看"),
            danmakuCount = formatCountText(item.stat?.danmaku, "弹幕"),
            likeCount = formatCountText(item.stat?.like, "点赞"),
            coinCount = formatCountText(item.stat?.coin, "投币"),
            fansCount = formatCountText(item.owner?.fans, "粉丝")
        )
    }

    private fun mapSearchItem(item: SearchResultItem): VideoCard {
        return VideoCard(
            id = item.bvid.orEmpty(),
            title = cleanText(item.title).ifBlank { "未命名视频" },
            author = cleanText(item.author).ifBlank { "未知UP主" },
            cover = normalizeUrl(upgradeImageUrl(item.pic.orEmpty())),
            duration = item.duration?.trim().orEmpty(),
            description = cleanText(item.description).ifBlank { "暂无简介" },
            category = cleanText(item.typeName).ifBlank { "搜索结果" },
            publishDate = formatDate(item.pubDate),
            viewCount = formatCountFromRaw(item.play, "观看"),
            danmakuCount = formatCountText(item.videoReview?.toLong(), "弹幕"),
            fansCount = "--粉丝"
        )
    }

    private fun mapHistoryItem(item: HistoryItem): VideoCard? {
        val bvid = item.bvid?.takeIf { it.isNotBlank() } ?: item.history?.bvid?.takeIf { it.isNotBlank() } ?: return null
        val cover = item.pic
            ?: item.cover
            ?: item.covers.firstOrNull().orEmpty()
        return VideoCard(
            id = bvid,
            title = cleanText(item.title).ifBlank { "未命名视频" },
            author = cleanText(item.authorName).ifBlank { "未知UP主" },
            cover = normalizeUrl(upgradeImageUrl(cover)),
            duration = formatSeconds(item.duration ?: item.history?.dt ?: 0),
            description = cleanText(item.desc).ifBlank { cleanText(item.history?.part).ifBlank { "历史记录" } },
            category = "历史记录",
            publishDate = formatDate(item.viewAt),
            avatar = normalizeUrl(upgradeImageUrl(item.authorFace.orEmpty()))
        )
    }

    private fun mapToViewItem(item: ToViewItem): VideoCard? {
        val bvid = item.bvid?.takeIf { it.isNotBlank() } ?: return null
        return VideoCard(
            id = bvid,
            title = cleanText(item.title).ifBlank { "未命名视频" },
            author = cleanText(item.owner?.name).ifBlank { "未知UP主" },
            cover = normalizeUrl(upgradeImageUrl(item.pic.orEmpty())),
            duration = formatSeconds(item.duration ?: 0),
            description = cleanText(item.desc).ifBlank { "稍后再看" },
            category = cleanText(item.tname).ifBlank { "稍后再看" },
            publishDate = formatDate(item.pubDate),
            avatar = normalizeUrl(upgradeImageUrl(item.owner?.face.orEmpty())),
            viewCount = formatCountText(item.stat?.view, "观看"),
            danmakuCount = formatCountText(item.stat?.danmaku, "弹幕"),
            likeCount = formatCountText(item.stat?.like, "点赞"),
            coinCount = formatCountText(item.stat?.coin, "投币")
        )
    }

    private fun mapFavoriteMedia(item: FavoriteMediaItem): VideoCard? {
        val bvid = item.bvid?.takeIf { it.isNotBlank() } ?: return null
        return VideoCard(
            id = bvid,
            title = cleanText(item.title).ifBlank { "未命名视频" },
            author = cleanText(item.upper?.name).ifBlank { "未知UP主" },
            cover = normalizeUrl(upgradeImageUrl(item.cover.orEmpty())),
            duration = formatSeconds(item.duration ?: 0),
            description = cleanText(item.intro).ifBlank { "收藏视频" },
            category = "收藏",
            publishDate = formatDate(item.pubTime),
            avatar = normalizeUrl(upgradeImageUrl(item.upper?.face.orEmpty())),
            viewCount = formatCountText(item.countInfo?.play, "观看"),
            danmakuCount = formatCountText(item.countInfo?.danmaku, "弹幕"),
            likeCount = formatCountText(item.countInfo?.like, "点赞"),
            coinCount = formatCountText(item.countInfo?.coin, "投币"),
            fansCount = formatCountText(item.countInfo?.collect, "收藏")
        )
    }

    private fun mapSpaceVideo(item: SpaceVideoItem): VideoCard? {
        val bvid = item.bvid?.takeIf { it.isNotBlank() } ?: return null
        return VideoCard(
            id = bvid,
            title = cleanText(item.title).ifBlank { "未命名视频" },
            author = cleanText(item.author).ifBlank { "我的视频" },
            cover = normalizeUrl(upgradeImageUrl(item.pic.orEmpty())),
            duration = item.length?.trim().orEmpty().ifBlank { "--:--" },
            description = cleanText(item.description).ifBlank { "我的视频" },
            category = "我的",
            publishDate = formatDate(item.created),
            viewCount = formatCountFromRaw(item.play, "观看"),
            danmakuCount = formatCountFromRaw(item.videoReview, "弹幕")
        )
    }

    private fun mapReplyItem(item: ReplyItem): VideoComment {
        val content = cleanText(item.content?.message)
        return VideoComment(
            id = item.replyId ?: 0L,
            author = cleanText(item.member?.userName).ifBlank { "匿名用户" },
            avatar = normalizeUrl(item.member?.avatar.orEmpty()),
            level = item.member?.levelInfo?.currentLevel ?: 0,
            content = content,
            likeCount = formatCountText(item.like, "赞"),
            replyCount = item.replies.size,
            publishTime = formatRelativeTime(item.ctime)
        )
    }

    private fun mapQualityOptions(ids: List<Int>, descriptions: List<String>): List<PlaybackQualityOption> {
        val options = ids.mapIndexed { index, qn ->
            PlaybackQualityOption(
                qn = qn,
                label = descriptions.getOrNull(index)?.ifBlank { null } ?: defaultQualityLabel(qn)
            )
        }
        return options
            .distinctBy { it.qn }
            .sortedByDescending { it.qn }
    }

    private suspend fun requestWbiPlayData(
        bvid: String,
        cid: Long,
        qn: Int,
        imgKey: String,
        subKey: String
    ): PlayUrlData {
        val params = BiliWbi.sign(
            params = mapOf(
                "bvid" to bvid,
                "cid" to cid,
                "qn" to qn,
                "fnval" to 4048,
                "fnver" to 0,
                "fourk" to 1
            ),
            imgKey = imgKey,
            subKey = subKey
        )
        return api.getPlayUrl(params).requireData("播放地址")
    }

    private suspend fun queryWbiKeys(): Pair<String, String>? {
        return runCatching {
            val nav = api.getNav().requireData("导航信息")
            val imgKey = nav.wbiImg?.imgUrl?.substringAfterLast('/')?.substringBefore('.')?.takeIf { it.isNotBlank() }
            val subKey = nav.wbiImg?.subUrl?.substringAfterLast('/')?.substringBefore('.')?.takeIf { it.isNotBlank() }
            if (imgKey.isNullOrBlank() || subKey.isNullOrBlank()) null else imgKey to subKey
        }.getOrNull()
    }

    private fun buildPlaybackInfoFromPlayData(
        playData: PlayUrlData,
        expectedQn: Int,
        title: String,
        subtitle: String,
        cover: String
    ): PlaybackInfo {
        val qualityOptions = mapQualityOptions(playData.acceptQuality, playData.acceptDescription)
        val currentQn = playData.quality ?: expectedQn
        val headers = mapOf(
            "Referer" to REFERER,
            "User-Agent" to USER_AGENT
        )

        val directUrl = playData.durl.firstNotNullOfOrNull { item ->
            firstAvailableUrl(
                primary = item.url,
                backups = item.backupUrl
            )
        }?.let(::normalizeUrl)
        if (!directUrl.isNullOrBlank()) {
            return PlaybackInfo(
                url = directUrl,
                audioUrl = null,
                title = title,
                subtitle = subtitle,
                cover = cover,
                headers = headers,
                isDash = false,
                currentQn = currentQn,
                qualityOptions = qualityOptions
            )
        }

        val videoCandidates = playData.dash?.video
            .orEmpty()
            .filter { firstAvailableUrl(it.baseUrl, listOf(it.baseUrlAlt) + it.backupUrl + it.backupUrlAlt) != null }
        if (videoCandidates.isEmpty()) error("未找到可播放视频流")

        val selectedVideo = videoCandidates
            .sortedWith(
                compareByDescending<DashStreamItem> { (it.id ?: 0) == currentQn || (it.id ?: 0) == expectedQn }
                    .thenByDescending { isAvcCodec(it.codecs) }
                    .thenByDescending { it.id ?: 0 }
                    .thenByDescending { it.bandwidth ?: 0L }
            )
            .first()
        val selectedVideoUrl = firstAvailableUrl(
            primary = selectedVideo.baseUrl,
            backups = listOf(selectedVideo.baseUrlAlt) + selectedVideo.backupUrl + selectedVideo.backupUrlAlt
        )?.let(::normalizeUrl) ?: error("视频流地址为空")

        val selectedAudio = playData.dash?.audio
            .orEmpty()
            .sortedByDescending { it.bandwidth ?: 0L }
            .firstOrNull { firstAvailableUrl(it.baseUrl, listOf(it.baseUrlAlt) + it.backupUrl + it.backupUrlAlt) != null }
        val selectedAudioUrl = selectedAudio?.let {
            firstAvailableUrl(
                primary = it.baseUrl,
                backups = listOf(it.baseUrlAlt) + it.backupUrl + it.backupUrlAlt
            )
        }?.let(::normalizeUrl)

        return PlaybackInfo(
            url = selectedVideoUrl,
            audioUrl = selectedAudioUrl,
            title = title,
            subtitle = subtitle,
            cover = cover,
            headers = headers,
            isDash = !selectedAudioUrl.isNullOrBlank(),
            currentQn = selectedVideo.id ?: currentQn,
            qualityOptions = qualityOptions
        )
    }

    private fun firstAvailableUrl(primary: String?, backups: List<String?>): String? {
        return (listOf(primary) + backups)
            .firstOrNull { !it.isNullOrBlank() }
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun isAvcCodec(codecs: String?): Boolean {
        val value = codecs.orEmpty().lowercase(Locale.getDefault())
        return value.contains("avc") || value.contains("h264")
    }

    private fun feedDurationText(item: FeedItem): String {
        val displayed = item.coverLeftText1?.trim().orEmpty()
        if (displayed.isNotBlank() && displayed.contains(":")) return displayed
        return formatSeconds(item.duration ?: 0)
    }

    private fun upgradeImageUrl(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return ""
        val withoutParams = trimmed.substringBefore('?')
        val atIndex = withoutParams.indexOf('@')
        val cleaned = if (atIndex > 0) withoutParams.substring(0, atIndex) else withoutParams
        return cleaned
    }

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("//") -> "https:$trimmed"
            trimmed.startsWith("http://") -> "https://" + trimmed.removePrefix("http://")
            trimmed.startsWith("https://") -> trimmed
            trimmed.isBlank() -> ""
            else -> "https://$trimmed"
        }
    }

    private fun cleanText(text: String?): String {
        return text
            .orEmpty()
            .replace(Regex("<[^>]*>"), "")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun decodeXmlText(text: String): String {
        return text
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#x20;", " ")
            .trim()
    }

    private fun formatSeconds(seconds: Long): String {
        if (seconds <= 0) return "--:--"
        val hours = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            "%02d:%02d:%02d".format(hours, mins, secs)
        } else {
            "%02d:%02d".format(mins, secs)
        }
    }

    private fun formatDate(timestampSeconds: Long?): String {
        if (timestampSeconds == null || timestampSeconds <= 0) return "日期未知"
        val normalized = when {
            timestampSeconds > 100_000_000_000L -> timestampSeconds / 1000
            else -> timestampSeconds
        }
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
        return formatter.format(Date(normalized * 1000))
    }

    private fun formatDateOrRecent(timestampSeconds: Long?): String {
        val resolved = formatDate(timestampSeconds)
        return if (resolved == "日期未知") {
            val nowSeconds = System.currentTimeMillis() / 1000
            formatDate(nowSeconds)
        } else {
            resolved
        }
    }

    private fun formatRelativeTime(timestampSeconds: Long?): String {
        if (timestampSeconds == null || timestampSeconds <= 0) return "刚刚"
        val nowSeconds = System.currentTimeMillis() / 1000
        val diff = (nowSeconds - timestampSeconds).coerceAtLeast(0)
        return when {
            diff < 60 -> "刚刚"
            diff < 3600 -> "${diff / 60}分钟前"
            diff < 86400 -> "${diff / 3600}小时前"
            diff < 86400 * 30 -> "${diff / 86400}天前"
            else -> formatDate(timestampSeconds)
        }
    }

    private fun formatCountText(count: Long?, suffix: String): String {
        return count?.let { "${formatCompactCount(it)}$suffix" } ?: "--$suffix"
    }

    private fun formatCountFromRaw(raw: String?, suffix: String): String {
        val value = raw?.replace(",", "")?.trim()
        return when {
            value.isNullOrBlank() -> "--$suffix"
            value.toLongOrNull() != null -> "${formatCompactCount(value.toLong())}$suffix"
            else -> "$value$suffix"
        }
    }

    private fun formatCompactCount(count: Long): String {
        return when {
            count >= 100_000_000 -> String.format(Locale.getDefault(), "%.1f亿", count / 100_000_000.0)
            count >= 10_000 -> String.format(Locale.getDefault(), "%.1f万", count / 10_000.0)
            else -> count.toString()
        }.removeSuffix(".0万").removeSuffix(".0亿")
    }

    private fun defaultQualityLabel(qn: Int): String {
        return when (qn) {
            127 -> "8K 超高清"
            126 -> "杜比视界"
            125 -> "HDR 真彩"
            120 -> "4K 超清"
            116 -> "1080P60"
            112 -> "1080P+"
            80 -> "1080P 高清"
            74 -> "720P60"
            64 -> "720P 高清"
            32 -> "480P 清晰"
            16 -> "360P 流畅"
            else -> "${qn}P"
        }
    }

    private fun mapQrPollResult(data: QrCodePollData): QrPollResult {
        val state = when (data.code) {
            0 -> QrPollState.SUCCESS
            86038 -> QrPollState.EXPIRED
            86090 -> QrPollState.SCANNED_WAIT_CONFIRM
            86101 -> QrPollState.WAITING_SCAN
            else -> QrPollState.UNKNOWN
        }
        val message = when (state) {
            QrPollState.SUCCESS -> "登录成功"
            QrPollState.EXPIRED -> "二维码已过期，请刷新"
            QrPollState.SCANNED_WAIT_CONFIRM -> "已扫码，请在手机上确认"
            QrPollState.WAITING_SCAN -> "请使用哔哩哔哩 App 扫码"
            QrPollState.UNKNOWN -> data.message?.ifBlank { "扫码状态未知" } ?: "扫码状态未知"
        }
        return QrPollResult(state = state, message = message)
    }

    private suspend fun resolveAidByBvid(bvid: String): Long {
        aidCache[bvid]?.let { return it }
        val detail = api.getVideoDetail(bvid).requireData("视频详情").view ?: error("未获取到视频详情")
        val aid = detail.aid ?: error("未获取到 aid")
        aidCache[bvid] = aid
        if (aidCache.size > 240) {
            val firstKey = aidCache.entries.firstOrNull()?.key
            if (!firstKey.isNullOrBlank()) aidCache.remove(firstKey)
        }
        return aid
    }

    private suspend fun requireCsrf(): String {
        val direct = synchronized(cookieLock) { cookieStore["bili_jct"].orEmpty() }
        if (direct.isNotBlank()) return direct
        if (!hasSessionCookie()) error("请先扫码登录")

        runCatching {
            val request = Request.Builder()
                .url("https://www.bilibili.com/")
                .header("User-Agent", USER_AGENT)
                .header("Referer", REFERER)
                .build()
            client.newCall(request).execute().use { }
        }

        val fromRefresh = synchronized(cookieLock) { cookieStore["bili_jct"].orEmpty() }
        if (fromRefresh.isNotBlank()) return fromRefresh

        runCatching { api.getNav() }

        val csrf = synchronized(cookieLock) { cookieStore["bili_jct"].orEmpty() }
        if (csrf.isBlank()) error("登录态缺少 csrf，请重新扫码登录")
        return csrf
    }

    private fun buildCookieHeader(): String {
        return synchronized(cookieLock) {
            if (cookieStore.isEmpty()) return@synchronized ""
            cookieStore.entries.joinToString("; ") { "${it.key}=${it.value}" }
        }
    }

    private fun saveCookieFromHeader(rawSetCookie: String) {
        val pair = rawSetCookie.substringBefore(';').trim()
        if (!pair.contains('=')) return
        val key = pair.substringBefore('=').trim()
        val value = pair.substringAfter('=').trim()
        if (key.isNotBlank()) {
            synchronized(cookieLock) {
                if (value.isBlank()) {
                    cookieStore.remove(key)
                } else {
                    cookieStore[key] = value
                }
            }
            persistCookiesToDisk()
        }
    }

    private fun restoreCookiesFromDisk() {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_COOKIES_JSON, "").orEmpty()
        if (json.isBlank()) return
        val type = object : TypeToken<Map<String, String>>() {}.type
        val parsed = runCatching {
            Gson().fromJson<Map<String, String>>(json, type).orEmpty()
        }.getOrDefault(emptyMap())
        synchronized(cookieLock) {
            cookieStore.clear()
            parsed
                .filterKeys { it.isNotBlank() }
                .forEach { (key, value) ->
                    if (value.isNotBlank()) {
                        cookieStore[key] = value
                    }
                }
        }
    }

    private fun persistCookiesToDisk() {
        val ctx = appContext ?: return
        val cookies = synchronized(cookieLock) { cookieStore.toMap() }
        val json = Gson().toJson(cookies)
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_COOKIES_JSON, json)
            .apply()
    }

    private fun String.containsAny(vararg tokens: String): Boolean {
        if (this.isBlank()) return false
        return tokens.any { token -> this.contains(token, ignoreCase = true) }
    }

    private fun <T> BiliResponse<T>.requireData(scene: String): T {
        if (code != 0) {
            error("$scene 请求失败: code=$code, message=$message")
        }
        return data ?: error("$scene 返回空数据")
    }
}
