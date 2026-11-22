# IGNGmc Android (v2.0)

纯 Kotlin + Jetpack Compose 多模块原生重构，采用 IGNG Bento 2.0 设计语言、折角核心加载体系与克制顺滑的物理动效。

## 模块架构

```text
android/
├── app/                     # 应用壳、Type-Safe Navigation、悬浮胶囊底栏、OAuth 回调、App Shortcuts、设置与多账号
├── core/
│   ├── model/               # 站点地址真源、格式化器、MC § 富文本解析、自定义勋章校验、官方社交渠道
│   ├── designsystem/        # IGNG Tokens (浅/深画布 + 5 家族纹理)、折角核心加载器、动效曲线、Bento 组件、原生图表与灯箱
│   ├── network/             # 单例 OkHttp 连接池、Bearer 注入、RSA-OAEP+AES-GCM 密码信封、401 统一恢复
│   ├── auth/                # PKCE S256、Chrome Custom Tabs OAuth 2.1、Android Keystore 多账号保险库
│   └── data/                # 全量 DTO、8 个业务 Repository、离线文件缓存、Coil 3 鉴权图片加载器
└── feature/
    ├── explore/             # 服务器家族/周目 Hero、周目档案、全局搜索、玩家通行证、社区大厅、完美之人
    ├── status/              # 公开状态墙、90 格健康条与手势交互 Bezier 趋势图
    ├── map/                 # 实时 Conflux 地图容器 + 历史周目静态快照原生缩放画布
    ├── chat/                # chat.igng.net MC/QQ 聊天记录、媒体灯箱、合并转发抽屉、官方交流群
    ├── wiki/                # docs.igng.net 离线百科目录与原生 Markdown 阅读器（含 TOC 抽屉）
    ├── player/              # dash.igng.net/mc 玩家个人控制中心全量 11 个子模块
    ├── ticket/              # 客服工单全生命周期、图片上传与预览、重开、本地草稿防丢失
    └── admin/               # 按角色动态解锁的口袋控制台（工单、审计、Historian 周报、流量与节点状态）
```

## 对接站点真源

默认对接 IGNG 单站拓扑（可通过 Gradle 属性覆写）：

- `IGNG_MC_BASE_URL`：`https://mc.igng.net`（服务器目录、公开状态与历史、全站搜索、玩家公开档案、社区大厅、完美之人、地图）
- `IGNG_DASH_BASE_URL`：`https://dash.igng.net`（MC 个人中心、工单与附件、跨服仓库、原版统计、勋章、权限、管理台、每日经验）
- `IGNG_SSO_BASE_URL`：`https://sso.igng.net`（OAuth 2.1 PKCE 授权、会话校验与登出）
- `IGNG_CHAT_BASE_URL`：`https://chat.igng.net`（MC 与 QQ 聊天频道、消息、媒体与合并转发）
- `IGNG_WIKI_BASE_URL`：`https://docs.igng.net`（IGNGmc 官方百科目录与正文）
- `IGNG_MAP_RELAY_URL`：`https://map-api.igng.net`（实时地图中继）

## 构建与单测

```bash
cd android
./gradlew assembleDebug testDebugUnitTest
```

可通过 Gradle 属性覆盖任意服务端地址：

```bash
./gradlew assembleDebug -PIGNG_MC_BASE_URL=https://mc.igng.net -PIGNG_DASH_BASE_URL=https://dash.igng.net
```
