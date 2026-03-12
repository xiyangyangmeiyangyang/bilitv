package com.openclaw.bilitv.data.model

import com.google.gson.annotations.SerializedName

data class BiliResponse<T>(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("ttl") val ttl: Int? = null,
    @SerializedName("data") val data: T?
)

data class FeedData(
    @SerializedName("item") val items: List<FeedItem> = emptyList()
)

data class FeedItem(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("bvid") val bvid: String? = null,
    @SerializedName("title") val title: String? = null,
    // 该接口返回字段名通常为 pic（历史上也出现过 cover），这里两者都兼容
    @SerializedName("pic") val cover: String? = null,
    @SerializedName("cover") val coverAlt: String? = null,
    @SerializedName("uri") val uri: String? = null,
    @SerializedName("goto") val gotoField: String? = null,
    @SerializedName("duration") val duration: Long? = null,
    @SerializedName("pubdate") val pubDate: Long? = null,
    @SerializedName("owner") val owner: FeedOwner? = null,
    @SerializedName("stat") val stat: FeedStat? = null,
    @SerializedName("args") val args: FeedArgs? = null,
    @SerializedName("player_args") val playerArgs: FeedPlayerArgs? = null,
    @SerializedName("rcmd_reason") val reason: FeedReason? = null,
    @SerializedName("cover_left_text_1") val coverLeftText1: String? = null,
    @SerializedName("cover_left_text_2") val coverLeftText2: String? = null,
    @SerializedName("desc") val desc: String? = null
)

data class FeedOwner(
    @SerializedName("mid") val mid: Long? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("face") val face: String? = null
)

data class FeedStat(
    @SerializedName("view") val view: Long? = null,
    @SerializedName("danmaku") val danmaku: Long? = null,
    @SerializedName("like") val like: Long? = null,
    @SerializedName("coin") val coin: Long? = null,
    @SerializedName("favorite") val favorite: Long? = null
)

data class FeedArgs(
    @SerializedName("up_name") val upName: String? = null
)

data class FeedPlayerArgs(
    @SerializedName("aid") val aid: Long? = null,
    @SerializedName("cid") val cid: Long? = null
)

data class FeedReason(
    @SerializedName("content") val content: String? = null
)

data class SearchResultData(
    @SerializedName("result") val result: List<SearchResultItem> = emptyList()
)

data class SearchDefaultData(
    @SerializedName("show_name") val showName: String? = null,
    @SerializedName("name") val name: String? = null
)

data class SearchSquareData(
    @SerializedName("trending") val trending: SearchTrendingData? = null
)

data class SearchTrendingData(
    @SerializedName("list") val list: List<TrendingKeywordItem> = emptyList()
)

data class TrendingKeywordItem(
    @SerializedName("keyword") val keyword: String? = null,
    @SerializedName("show_name") val showName: String? = null,
    @SerializedName("hot_id") val hotId: Long? = null
)

data class PopularData(
    @SerializedName("list") val list: List<FeedItem> = emptyList()
)

data class PopularPreciousData(
    @SerializedName("title") val title: String? = null,
    @SerializedName("media_id") val mediaId: Long? = null,
    @SerializedName("explain") val explain: String? = null,
    @SerializedName("list") val list: List<FeedItem> = emptyList()
)

data class PopularSeriesListData(
    @SerializedName("list") val list: List<PopularSeriesItem> = emptyList()
)

data class PopularSeriesItem(
    @SerializedName("number") val number: Int? = null,
    @SerializedName("subject") val subject: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("status") val status: Int? = null
)

data class PopularSeriesOneData(
    @SerializedName("list") val list: List<FeedItem> = emptyList()
)

data class NewListData(
    @SerializedName("archives") val archives: List<FeedItem> = emptyList()
)

data class RankingData(
    @SerializedName("note") val note: String? = null,
    @SerializedName("list") val list: List<RankingItem> = emptyList()
)

data class RankingV2Data(
    @SerializedName("note") val note: String? = null,
    @SerializedName("list") val list: List<FeedItem> = emptyList()
)

data class RankingItem(
    @SerializedName("aid") val aid: String? = null,
    @SerializedName("bvid") val bvid: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("author") val author: String? = null,
    @SerializedName("pic") val pic: String? = null,
    @SerializedName("duration") val duration: String? = null,
    @SerializedName("desc") val desc: String? = null,
    @SerializedName("tname") val tname: String? = null,
    @SerializedName("pubdate") val pubDate: Long? = null,
    @SerializedName("play") val play: String? = null,
    @SerializedName("video_review") val videoReview: String? = null,
    @SerializedName("coins") val coins: String? = null
)

data class SearchResultItem(
    @SerializedName("aid") val aid: Long? = null,
    @SerializedName("bvid") val bvid: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("author") val author: String? = null,
    @SerializedName("pic") val pic: String? = null,
    @SerializedName("duration") val duration: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("typename") val typeName: String? = null,
    @SerializedName("pubdate") val pubDate: Long? = null,
    @SerializedName("play") val play: String? = null,
    @SerializedName("video_review") val videoReview: Int? = null,
    @SerializedName("like") val like: Long? = null
)

data class VideoDetailData(
    @SerializedName("View") val view: VideoView? = null,
    @SerializedName("Related") val related: List<RelatedVideoItem> = emptyList()
)

data class VideoView(
    @SerializedName("aid") val aid: Long? = null,
    @SerializedName("bvid") val bvid: String? = null,
    @SerializedName("cid") val cid: Long? = null,
    @SerializedName("pages") val pages: List<PageItem> = emptyList(),
    @SerializedName("title") val title: String? = null,
    @SerializedName("pic") val pic: String? = null,
    @SerializedName("desc") val desc: String? = null,
    @SerializedName("duration") val duration: Long? = null,
    @SerializedName("tname") val tname: String? = null,
    @SerializedName("pubdate") val pubDate: Long? = null,
    @SerializedName("stat") val stat: VideoStat? = null,
    @SerializedName("owner") val owner: VideoOwner? = null
)

data class VideoOwner(
    @SerializedName("mid") val mid: Long? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("face") val face: String? = null,
    @SerializedName("fans") val fans: Long? = null
)

data class VideoStat(
    @SerializedName("view") val view: Long? = null,
    @SerializedName("danmaku") val danmaku: Long? = null,
    @SerializedName("like") val like: Long? = null,
    @SerializedName("coin") val coin: Long? = null,
    @SerializedName("favorite") val favorite: Long? = null
)

data class RelatedVideoItem(
    @SerializedName("aid") val aid: Long? = null,
    @SerializedName("bvid") val bvid: String? = null,
    @SerializedName("cid") val cid: Long? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("pic") val pic: String? = null,
    @SerializedName("desc") val desc: String? = null,
    @SerializedName("duration") val duration: Long? = null,
    @SerializedName("owner") val owner: VideoOwner? = null,
    @SerializedName("stat") val stat: VideoStat? = null,
    @SerializedName("pubdate") val pubDate: Long? = null,
    @SerializedName("tname") val tname: String? = null
)

data class ReplyData(
    @SerializedName("page") val page: ReplyPage? = null,
    @SerializedName("replies") val replies: List<ReplyItem> = emptyList()
)

data class ReplyPage(
    @SerializedName("acount") val allCount: Long? = null,
    @SerializedName("count") val pageCount: Long? = null,
    @SerializedName("num") val pageNum: Int? = null,
    @SerializedName("size") val pageSize: Int? = null
)

data class ReplyItem(
    @SerializedName("rpid") val replyId: Long? = null,
    @SerializedName("ctime") val ctime: Long? = null,
    @SerializedName("like") val like: Long? = null,
    @SerializedName("member") val member: ReplyMember? = null,
    @SerializedName("content") val content: ReplyContent? = null,
    @SerializedName("replies") val replies: List<ReplyItem> = emptyList()
)

data class ReplyMember(
    @SerializedName("mid") val mid: String? = null,
    @SerializedName("uname") val userName: String? = null,
    @SerializedName("avatar") val avatar: String? = null,
    @SerializedName("level_info") val levelInfo: ReplyLevelInfo? = null
)

data class ReplyLevelInfo(
    @SerializedName("current_level") val currentLevel: Int? = null
)

data class ReplyContent(
    @SerializedName("message") val message: String? = null
)

data class QrCodeGenerateData(
    @SerializedName("url") val url: String? = null,
    @SerializedName("qrcode_key") val qrcodeKey: String? = null
)

data class QrCodePollData(
    @SerializedName("url") val url: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("timestamp") val timestamp: Long? = null,
    @SerializedName("code") val code: Int? = null,
    @SerializedName("message") val message: String? = null
)

data class FavoriteFolderListData(
    @SerializedName("list") val list: List<FavoriteFolderItem> = emptyList()
)

data class FavoriteFolderItem(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("media_count") val mediaCount: Int? = null
)

data class VideoComment(
    val id: Long,
    val author: String,
    val avatar: String,
    val level: Int,
    val content: String,
    val likeCount: String,
    val replyCount: Int,
    val publishTime: String
)
