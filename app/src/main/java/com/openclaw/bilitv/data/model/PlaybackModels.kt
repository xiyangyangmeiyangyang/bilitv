package com.openclaw.bilitv.data.model

import com.google.gson.annotations.SerializedName

data class NavData(
    @SerializedName("wbi_img") val wbiImg: WbiImg? = null,
    @SerializedName("isLogin") val isLogin: Boolean? = null,
    @SerializedName("is_login") val isLoginInt: Int? = null,
    @SerializedName("uname") val uname: String? = null,
    @SerializedName("face") val face: String? = null,
    @SerializedName("mid") val mid: Long? = null,
    @SerializedName("money") val money: Double? = null,
    @SerializedName("level_info") val levelInfo: NavLevelInfo? = null
)

data class NavLevelInfo(
    @SerializedName("current_level") val currentLevel: Int? = null
)

data class WbiImg(
    @SerializedName("img_url") val imgUrl: String? = null,
    @SerializedName("sub_url") val subUrl: String? = null
)

data class PageItem(
    @SerializedName("cid") val cid: Long? = null,
    @SerializedName("page") val page: Int? = null,
    @SerializedName("part") val part: String? = null,
    @SerializedName("duration") val duration: Long? = null
)

data class PlayUrlData(
    @SerializedName("dash") val dash: DashData? = null,
    @SerializedName("durl") val durl: List<DurlItem> = emptyList(),
    @SerializedName("quality") val quality: Int? = null,
    @SerializedName("timelength") val timelength: Long? = null,
    @SerializedName("accept_quality") val acceptQuality: List<Int> = emptyList(),
    @SerializedName("accept_description") val acceptDescription: List<String> = emptyList()
)

data class DashData(
    @SerializedName("duration") val duration: Long? = null,
    @SerializedName("minBufferTime") val minBufferTime: Double? = null,
    @SerializedName("video") val video: List<DashStreamItem> = emptyList(),
    @SerializedName("audio") val audio: List<DashStreamItem> = emptyList()
)

data class DashStreamItem(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("baseUrl") val baseUrl: String? = null,
    @SerializedName("base_url") val baseUrlAlt: String? = null,
    @SerializedName("backupUrl") val backupUrl: List<String> = emptyList(),
    @SerializedName("backup_url") val backupUrlAlt: List<String> = emptyList(),
    @SerializedName("codecs") val codecs: String? = null,
    @SerializedName("mimeType") val mimeType: String? = null,
    @SerializedName("mime_type") val mimeTypeAlt: String? = null,
    @SerializedName("bandwidth") val bandwidth: Long? = null,
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null
)

data class DurlItem(
    @SerializedName("url") val url: String? = null,
    @SerializedName("backup_url") val backupUrl: List<String> = emptyList(),
    @SerializedName("length") val length: Long? = null,
    @SerializedName("size") val size: Long? = null
)

data class PlaybackInfo(
    val url: String,
    val audioUrl: String? = null,
    val title: String,
    val subtitle: String,
    val cover: String,
    val headers: Map<String, String> = emptyMap(),
    val isDash: Boolean = false,
    val currentQn: Int = 0,
    val qualityOptions: List<PlaybackQualityOption> = emptyList()
)

data class PlaybackQualityOption(
    val qn: Int,
    val label: String
)
