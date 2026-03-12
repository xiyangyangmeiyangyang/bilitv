package com.openclaw.bilitv.data.api

import com.openclaw.bilitv.data.model.BiliResponse
import com.openclaw.bilitv.data.model.FeedData
import com.openclaw.bilitv.data.model.FavoriteFolderListData
import com.openclaw.bilitv.data.model.FavoriteResourceListData
import com.openclaw.bilitv.data.model.HistoryData
import com.openclaw.bilitv.data.model.NavData
import com.openclaw.bilitv.data.model.NewListData
import com.openclaw.bilitv.data.model.OnlineRegionData
import com.openclaw.bilitv.data.model.PageItem
import com.openclaw.bilitv.data.model.PlayUrlData
import com.openclaw.bilitv.data.model.PopularData
import com.openclaw.bilitv.data.model.PopularPreciousData
import com.openclaw.bilitv.data.model.PopularSeriesListData
import com.openclaw.bilitv.data.model.PopularSeriesOneData
import com.openclaw.bilitv.data.model.QrCodeGenerateData
import com.openclaw.bilitv.data.model.QrCodePollData
import com.openclaw.bilitv.data.model.RankingData
import com.openclaw.bilitv.data.model.RankingV2Data
import com.openclaw.bilitv.data.model.RelationStatData
import com.openclaw.bilitv.data.model.ReplyData
import com.openclaw.bilitv.data.model.SearchDefaultData
import com.openclaw.bilitv.data.model.SearchResultData
import com.openclaw.bilitv.data.model.SearchSuggestResponse
import com.openclaw.bilitv.data.model.SearchSquareData
import com.openclaw.bilitv.data.model.SpaceArchiveSearchData
import com.openclaw.bilitv.data.model.ToViewData
import com.openclaw.bilitv.data.model.VideoDetailData
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface BiliApiService {
    @GET("x/web-interface/index/top/feed/rcmd")
    suspend fun getHomeFeed(
        @Query("ps") pageSize: Int = 8,
        @Query("fresh_type") freshType: Int = 3,
        @Query("fresh_idx") freshIndex: Int = 1,
        @Query("fresh_idx_1h") freshIndex1h: Int = 1
    ): BiliResponse<FeedData>

    @GET("x/web-interface/popular")
    suspend fun getPopularVideos(
        @Query("pn") page: Int = 1,
        @Query("ps") pageSize: Int = 20
    ): BiliResponse<PopularData>

    @GET("x/web-interface/popular/precious")
    suspend fun getPopularPreciousVideos(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): BiliResponse<PopularPreciousData>

    @GET("x/web-interface/popular/series/list")
    suspend fun getPopularSeriesList(): BiliResponse<PopularSeriesListData>

    @GET("x/web-interface/popular/series/one")
    suspend fun getPopularSeriesOne(
        @QueryMap(encoded = true) params: Map<String, String>
    ): BiliResponse<PopularSeriesOneData>

    @GET("x/web-interface/newlist")
    suspend fun getNewVideos(
        @Query("rid") rid: Int? = null,
        @Query("pn") page: Int = 1,
        @Query("ps") pageSize: Int = 20
    ): BiliResponse<NewListData>

    @GET("x/web-interface/ranking")
    suspend fun getRankingVideos(
        @Query("rid") regionId: Int = 0,
        @Query("day") day: Int = 3,
        @Query("type") type: Int = 1
    ): BiliResponse<RankingData>

    @GET("x/web-interface/ranking/v2")
    suspend fun getRankingVideosV2(
        @Query("rid") regionId: Int = 0,
        @Query("type") type: String = "all"
    ): BiliResponse<RankingV2Data>

    @GET("x/web-interface/wbi/search/type")
    suspend fun searchVideos(
        @Query("search_type") searchType: String = "video",
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 24
    ): BiliResponse<SearchResultData>

    @GET("x/web-interface/search/default")
    suspend fun getSearchDefaultKeyword(): BiliResponse<SearchDefaultData>

    @GET("x/web-interface/search/square")
    suspend fun getSearchTrendingKeywords(
        @Query("limit") limit: Int = 10
    ): BiliResponse<SearchSquareData>

    @GET("x/web-interface/view/detail")
    suspend fun getVideoDetail(
        @Query("bvid") bvid: String
    ): BiliResponse<VideoDetailData>

    @GET("x/web-interface/nav")
    suspend fun getNav(): BiliResponse<NavData>

    @GET("x/web-interface/online")
    suspend fun getOnlineRegions(): BiliResponse<OnlineRegionData>

    @GET("x/player/pagelist")
    suspend fun getPageList(
        @Query("bvid") bvid: String,
        @Query("jsonp") jsonp: String = "jsonp"
    ): BiliResponse<List<PageItem>>

    @GET("x/player/wbi/playurl")
    suspend fun getPlayUrl(
        @QueryMap(encoded = true) params: Map<String, String>
    ): BiliResponse<PlayUrlData>

    @GET("x/player/playurl")
    suspend fun getPlayUrlLegacy(
        @Query("bvid") bvid: String,
        @Query("cid") cid: Long,
        @Query("qn") qn: Int = 64,
        @Query("fnval") fnval: Int = 0,
        @Query("fnver") fnver: Int = 0,
        @Query("fourk") fourk: Int = 0,
        @Query("otype") otype: String = "json"
    ): BiliResponse<PlayUrlData>

    @GET("x/v2/reply")
    suspend fun getVideoReplies(
        @Query("oid") oid: Long,
        @Query("type") type: Int = 1,
        @Query("pn") page: Int = 1,
        @Query("ps") pageSize: Int = 20,
        @Query("sort") sort: Int = 2
    ): BiliResponse<ReplyData>

    @GET("x/v2/history")
    suspend fun getWatchHistory(
        @Query("pn") page: Int = 1,
        @Query("ps") pageSize: Int = 20
    ): BiliResponse<HistoryData>

    @GET("x/web-interface/history/cursor")
    suspend fun getWatchHistoryCursor(
        @Query("ps") pageSize: Int = 20,
        @Query("max") max: Long = 0,
        @Query("view_at") viewAt: Long = 0,
        @Query("business") business: String = "archive"
    ): BiliResponse<com.openclaw.bilitv.data.model.HistoryCursorData>

    @GET("x/v2/history/toview/web")
    suspend fun getWatchLaterList(): BiliResponse<ToViewData>

    @FormUrlEncoded
    @POST("x/web-interface/archive/like")
    suspend fun setVideoLike(
        @Field("aid") aid: Long,
        @Field("bvid") bvid: String,
        @Field("like") like: Int,
        @Field("csrf") csrf: String,
        @Field("csrf_token") csrfToken: String,
        @Field("source") source: String = "web_normal"
    ): BiliResponse<Any>

    @FormUrlEncoded
    @POST("x/web-interface/coin/add")
    suspend fun addVideoCoin(
        @Field("aid") aid: Long,
        @Field("bvid") bvid: String,
        @Field("multiply") multiply: Int,
        @Field("select_like") selectLike: Int,
        @Field("csrf") csrf: String,
        @Field("csrf_token") csrfToken: String
    ): BiliResponse<Any>

    @GET("x/v3/fav/folder/created/list-all")
    suspend fun getFavoriteFolders(
        @Query("type") type: Int = 2,
        @Query("rid") rid: Long
    ): BiliResponse<FavoriteFolderListData>

    @GET("x/v3/fav/folder/created/list-all")
    suspend fun getCreatedFavoriteFolders(
        @Query("up_mid") upMid: Long,
        @Query("type") type: Int = 2
    ): BiliResponse<FavoriteFolderListData>

    @GET("x/v3/fav/resource/list")
    suspend fun getFavoriteFolderResources(
        @Query("media_id") mediaId: Long,
        @Query("pn") page: Int = 1,
        @Query("ps") pageSize: Int = 20,
        @Query("platform") platform: String = "web",
        @Query("type") type: Int = 0
    ): BiliResponse<FavoriteResourceListData>

    @FormUrlEncoded
    @POST("x/v3/fav/resource/deal")
    suspend fun dealFavoriteResource(
        @Field("rid") rid: Long,
        @Field("type") type: Int = 2,
        @Field("add_media_ids") addMediaIds: String = "",
        @Field("del_media_ids") delMediaIds: String = "",
        @Field("csrf") csrf: String,
        @Field("csrf_token") csrfToken: String
    ): BiliResponse<Any>

    @FormUrlEncoded
    @POST("x/v2/history/report")
    suspend fun reportHistoryProgress(
        @Field("aid") aid: Long,
        @Field("cid") cid: Long,
        @Field("progress") progress: Long? = null,
        @Field("platform") platform: String = "android",
        @Field("csrf") csrf: String,
        @Field("csrf_token") csrfToken: String? = null
    ): BiliResponse<Any>

    @FormUrlEncoded
    @POST("x/v2/history/report")
    suspend fun reportHistoryProgressSimple(
        @Field("aid") aid: Long,
        @Field("cid") cid: Long,
        @Field("progress") progress: Long? = null,
        @Field("platform") platform: String = "android",
        @Field("csrf") csrf: String
    ): BiliResponse<Any>

    @FormUrlEncoded
    @POST("x/relation/modify")
    suspend fun modifyRelation(
        @Field("fid") targetMid: Long,
        @Field("act") action: Int = 1,
        @Field("re_src") reSrc: Int = 11,
        @Field("csrf") csrf: String,
        @Field("csrf_token") csrfToken: String
    ): BiliResponse<Any>

    @GET("x/relation/stat")
    suspend fun getRelationStat(
        @Query("vmid") targetMid: Long
    ): BiliResponse<RelationStatData>

    @GET("x/space/wbi/arc/search")
    suspend fun getSpaceVideos(
        @QueryMap(encoded = true) params: Map<String, String>
    ): BiliResponse<SpaceArchiveSearchData>
}

interface BiliPassportApiService {
    @GET("x/passport-login/web/qrcode/generate")
    suspend fun generateQrCode(): BiliResponse<QrCodeGenerateData>

    @GET("x/passport-login/web/qrcode/poll")
    suspend fun pollQrCode(
        @Query("qrcode_key") qrCodeKey: String
    ): Response<BiliResponse<QrCodePollData>>
}

interface BiliSuggestApiService {
    @GET("main/suggest")
    suspend fun getSearchSuggest(
        @Query("term") term: String
    ): SearchSuggestResponse
}
