package com.openclaw.bilitv.model

data class VideoCard(
    val id: String,
    val title: String,
    val author: String,
    val cover: String,
    val duration: String,
    val description: String,
    val category: String,
    val publishDate: String = "",
    val avatar: String = "",
    val viewCount: String = "",
    val danmakuCount: String = "",
    val fansCount: String = "",
    val likeCount: String = "",
    val coinCount: String = "",
)
