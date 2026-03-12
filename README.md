# BiliTV

一个面向 Android TV 的哔哩哔哩客户端，重点做遥控器交互、真实数据接入和全屏播放体验。

## 功能概览

- 首页内容流：推荐、热门、收藏、搜索、全部分区、历史记录、我的
- 热门子页：综合热门、每周必看、排行榜、全站音乐榜
- 全部分区目录 + 分区视频列表
- 搜索：默认词、热词、联想词、系统输入弹层
- 播放器：全屏播放、分 P、清晰度、弹幕、自动连播、播放队列
- 互动：点赞、投币、收藏、关注（登录后）
- 账号：扫码登录、用户信息展示、退出登录
- 设置：主页磨砂强度、轮播间隔、播放器控件停留时间、背景主题

## 技术栈

- Kotlin
- Jetpack Compose / Compose for TV
- Navigation Compose
- Retrofit + OkHttp + Gson
- Media3 ExoPlayer
- Coil

## 项目结构

```text
app/src/main/java/com/openclaw/bilitv
├── MainActivity.kt                  # 入口、路由、按键分发
├── data
│   ├── api/BiliApiService.kt        # 接口定义
│   ├── model/*                      # 数据模型
│   └── repository/BiliRepository.kt # 数据聚合/回退/映射
├── model/VideoCard.kt               # UI 卡片模型
└── ui
    ├── screens/*                    # Home/Search/Player/Settings 等页面
    ├── player/*                     # 播放队列、键位调度
    ├── settings/*                   # UI 设置状态
    └── components/*                 # 公共组件
```

## 环境要求

- Android Studio（建议最新稳定版）
- JDK 17
- Android SDK 35
- 设备：Android TV（推荐已开启 ADB）

## 本地构建

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebugV7a
```

产物：

```text
app/build/outputs/apk/debugV7a/app-debugV7a.apk
```

## 安装到电视（示例）

```bash
/Users/l/Library/Android/sdk/platform-tools/adb connect 192.168.1.5:5555
/Users/l/Library/Android/sdk/platform-tools/adb -s 192.168.1.5:5555 install -r -d app/build/outputs/apk/debugV7a/app-debugV7a.apk
/Users/l/Library/Android/sdk/platform-tools/adb -s 192.168.1.5:5555 shell monkey -p com.openclaw.bilitv -c android.intent.category.LAUNCHER 1
```

## 数据与接口说明

- 项目默认优先真实接口，不走本地 mock 作为主链路。
- 关键接口覆盖：首页推荐、热门、分区、搜索、详情、播放、历史读取/上报。
- 分区列表内置多级回退策略，尽量避免空列表。
- 登录态依赖 Cookie（`SESSDATA`），写接口依赖 `bili_jct`/`csrf`。

## 当前状态

当前版本已具备“可用主链路”：

- 浏览 -> 详情/直接播放 -> 连续播放 -> 返回
- 搜索 -> 结果列表 -> 播放
- 扫码登录 -> 互动操作 -> 设置调节

仍在持续优化：

- 弹幕渲染性能和时序稳定性
- 历史写回一致性
- 分区接口在 B 站侧变化时的适配
- 遥控器焦点与返回链路细节

## 合规与免责声明

- 本项目为第三方客户端，不隶属于哔哩哔哩官方。
- 请仅在合法合规范围内使用，遵守相关平台协议与当地法律法规。
- 若接口策略变更导致功能异常，请以最新代码与实际接口状态为准。

