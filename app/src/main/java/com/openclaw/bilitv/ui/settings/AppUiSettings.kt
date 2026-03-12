package com.openclaw.bilitv.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

object AppUiSettings {
    // 主页背景磨砂强度，取值 0..1
    var homeFrostStrength by mutableFloatStateOf(0f)

    // 推荐区滚动到全屏时使用的渐变主题（默认蓝色）
    var recommendationFullscreenTheme by mutableIntStateOf(0)

    // 首页轮播自动切换间隔（秒）
    var homeCarouselSeconds by mutableIntStateOf(4)

    // 播放器控制层自动隐藏时间（秒）
    var playerOverlayAutoHideSeconds by mutableIntStateOf(4)
}
