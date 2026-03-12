# BiliTV 项目介绍

更新时间：2026-03-12

## 1. 项目定位

BiliTV 是一个面向 Android TV 的哔哩哔哩客户端，核心目标是：

- 以真实接口和真实账号数据为主
- 遥控器/键盘优先的电视交互体验
- 首页浏览 + 全屏播放器 + 分区检索的一体化流程

## 2. 技术栈

- 语言：Kotlin
- UI：Jetpack Compose + Compose for TV
- 导航：Navigation Compose
- 网络：Retrofit + OkHttp + Gson
- 播放：Media3 ExoPlayer
- 图片加载：Coil

## 3. 主要功能

### 首页与内容流

- 首页主标签：推荐、热门、收藏、搜索、全部分区、历史记录、我的
- 热门子标签：综合热门、每周必看、排行榜、全站音乐榜
- 推荐区 4 列网格展示，支持继续下滑加载
- 详情弹层支持从首页直接打开

### 分区系统

- 全部分区目录页（仅目录）
- 分区视频列表页（按分区进入并持续加载）
- 分区内容采用多级回退，尽量保证有真实内容可展示

### 搜索

- 默认词、热词、联想词
- 屏幕键盘输入
- 系统输入弹层（支持中文/英文/数字/符号）

### 播放器

- 全屏播放、自动连播、播放队列（上一条/下一条）
- 分 P 播放
- 清晰度切换
- 弹幕开关与参数控制（密度、速度、透明度、字号等）
- 点赞、投币、收藏、关注等互动入口

### 账号与设置

- 二维码登录/轮询登录状态
- 账号信息展示与退出登录
- 首页磨砂强度调节
- 首页轮播间隔调节
- 播放器控件停留时间调节
- 推荐区全屏背景主题切换

## 4. 数据与接口策略

项目默认优先真实接口，不使用本地 mock 列表作为主数据源。数据访问统一通过 `BiliRepository` 聚合。

### 已接入的核心链路

- 首页推荐：`x/web-interface/index/top/feed/rcmd`
- 热门：`x/web-interface/popular`
- 每周必看：`x/web-interface/popular/series/list` + `x/web-interface/popular/series/one`
- 分区：`x/web-interface/newlist`
- 搜索：`x/web-interface/wbi/search/type`
- 详情：`x/web-interface/view/detail`
- 分 P：`x/player/pagelist`
- 播放：`x/player/wbi/playurl` + `x/player/playurl`（回退）
- 历史读取：`x/web-interface/history/cursor` + `x/v2/history`（回退）
- 历史上报：`x/v2/history/report`

### 分区回退策略（保证可用性）

当分区主接口结果为空时，按顺序回退：

1. 主分区 `newlist`
2. 子分区 `newlist` 合并
3. 分区关键词搜索回退
4. 排行回退

### 登录态与写接口

- 读接口登录态：`SESSDATA`
- 写接口 CSRF：`bili_jct` + `csrf/csrf_token`
- 历史写回采用多参数组合重试，提升兼容性

## 5. 工程结构

```text
app/src/main/java/com/openclaw/bilitv
├── MainActivity.kt                  # 入口、路由、全局按键分发
├── data
│   ├── api/BiliApiService.kt        # 接口定义
│   ├── model/*                      # 网络与业务数据模型
│   └── repository/BiliRepository.kt # 数据聚合、映射、回退策略
├── model/VideoCard.kt               # UI 主卡片模型
└── ui
    ├── screens/*                    # Home/Search/Player/Settings 等页面
    ├── player/*                     # 播放队列与播放器按键调度
    ├── settings/*                   # UI 设置状态
    └── components/*                 # 通用组件
```

## 6. 运行与构建

### 本地编译

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebugV7a
```

### 安装到 Android TV（示例）

```bash
/Users/l/Library/Android/sdk/platform-tools/adb -s 192.168.1.5:5555 install -r -d app/build/outputs/apk/debugV7a/app-debugV7a.apk
/Users/l/Library/Android/sdk/platform-tools/adb -s 192.168.1.5:5555 shell monkey -p com.openclaw.bilitv -c android.intent.category.LAUNCHER 1
```

## 7. 当前状态与后续优化方向

当前版本已经形成“可用主链路”（浏览、搜索、播放、登录、设置）。后续重点仍在：

- 弹幕渲染性能与时序稳定性（高密度场景）
- 历史写回在不同账号状态下的一致性验证
- 分区/专题接口在 B 站侧变化时的动态适配
- 遥控器焦点与返回链路的持续打磨

## 8. 说明

- 本文档定位为“项目介绍”，用于快速理解项目现状和结构。
- 详细变更请结合 `git` 提交记录与代码实现查看。
