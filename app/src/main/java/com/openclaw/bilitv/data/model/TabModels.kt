package com.openclaw.bilitv.data.model

import com.google.gson.annotations.SerializedName

data class OnlineRegionData(
    @SerializedName("region_count") val regionCount: Map<String, Int> = emptyMap()
)

data class RelationStatData(
    @SerializedName("mid") val mid: Long? = null,
    @SerializedName("attribute") val attribute: Int? = null,
    @SerializedName("follower") val follower: Long? = null,
    @SerializedName("following") val following: Long? = null
)

data class HistoryData(
    @SerializedName("list") val list: List<HistoryItem> = emptyList()
)

data class HistoryCursorData(
    @SerializedName("list") val list: List<HistoryItem> = emptyList(),
    @SerializedName("cursor") val cursor: HistoryCursor? = null
)

data class HistoryCursor(
    @SerializedName("max") val max: Long? = null,
    @SerializedName("view_at") val viewAt: Long? = null
)

data class HistoryItem(
    @SerializedName("bvid") val bvid: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("pic") val pic: String? = null,
    @SerializedName("cover") val cover: String? = null,
    @SerializedName("covers") val covers: List<String> = emptyList(),
    @SerializedName("duration") val duration: Long? = null,
    @SerializedName("desc") val desc: String? = null,
    @SerializedName("author_name") val authorName: String? = null,
    @SerializedName("author_face") val authorFace: String? = null,
    @SerializedName("view_at") val viewAt: Long? = null,
    @SerializedName("history") val history: HistoryNode? = null
)

data class HistoryNode(
    @SerializedName("bvid") val bvid: String? = null,
    @SerializedName("part") val part: String? = null,
    @SerializedName("dt") val dt: Long? = null
)

data class ToViewData(
    @SerializedName("count") val count: Int? = null,
    @SerializedName("list") val list: List<ToViewItem> = emptyList()
)

data class ToViewItem(
    @SerializedName("bvid") val bvid: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("pic") val pic: String? = null,
    @SerializedName("desc") val desc: String? = null,
    @SerializedName("duration") val duration: Long? = null,
    @SerializedName("pubdate") val pubDate: Long? = null,
    @SerializedName("tname") val tname: String? = null,
    @SerializedName("owner") val owner: VideoOwner? = null,
    @SerializedName("stat") val stat: VideoStat? = null
)

data class FavoriteResourceListData(
    @SerializedName("medias") val medias: List<FavoriteMediaItem> = emptyList()
)

data class FavoriteMediaItem(
    @SerializedName("bvid") val bvid: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("cover") val cover: String? = null,
    @SerializedName("intro") val intro: String? = null,
    @SerializedName("duration") val duration: Long? = null,
    @SerializedName("pubtime") val pubTime: Long? = null,
    @SerializedName("upper") val upper: FavoriteUpper? = null,
    @SerializedName("cnt_info") val countInfo: FavoriteCountInfo? = null
)

data class FavoriteUpper(
    @SerializedName("name") val name: String? = null,
    @SerializedName("face") val face: String? = null
)

data class FavoriteCountInfo(
    @SerializedName("play") val play: Long? = null,
    @SerializedName("danmaku") val danmaku: Long? = null,
    @SerializedName("thumb_up") val like: Long? = null,
    @SerializedName("collect") val collect: Long? = null,
    @SerializedName("coin") val coin: Long? = null
)

data class SpaceArchiveSearchData(
    @SerializedName("list") val list: SpaceArchiveList? = null
)

data class SpaceArchiveList(
    @SerializedName("vlist") val vlist: List<SpaceVideoItem> = emptyList()
)

data class SpaceVideoItem(
    @SerializedName("bvid") val bvid: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("pic") val pic: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("length") val length: String? = null,
    @SerializedName("created") val created: Long? = null,
    @SerializedName("author") val author: String? = null,
    @SerializedName("play") val play: String? = null,
    @SerializedName("video_review") val videoReview: String? = null
)
