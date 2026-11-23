# IGNGmc Android

纯 Kotlin + Jetpack Compose 14 模块原生 Android 工程，采用 IGNG Bento 2.0 设计语言、折角核心加载体系与克制顺滑的物理动效。

## 模块架构

```text
android/
├── app/                     # 应用壳、Type-Safe Navigation、悬浮胶囊底栏、OAuth 回调、App Shortcuts、设置与多账号
├── core/
│   ├── model/               # 站点端点配置、格式化器、MC § 富文本解析、自定义勋章校验、官方社交渠道
│   ├── designsystem/        # IGNG Tokens (浅/深画布 + 5 家族纹理)、折角核心加载器、动效曲线、Bento 组件、原生图表与灯箱
│   ├── network/             # 单例 OkHttp 连接池、受信任域 Bearer 注入、Single-Flight 401 刷新、RSA-OAEP+AES-GCM 密码信封
│   ├── auth/                # PKCE S256、Chrome Custom Tabs OAuth 2.1、Android Keystore 多账号保险库
│   └── data/                # Client API v1 全量 DTO、黄金契约样本、业务 Repository、离线文件缓存、Coil 3 鉴权图片加载器
└── feature/
    ├── explore/             # 服务器家族/周目 Hero、周目档案、全局搜索、玩家通行证、社区大厅、完美之人
    ├── status/              # 公开状态墙、健康记录时间轴与手势交互 Bezier 趋势图
    ├── map/                 # 实时/归档 Conflux 地图容器 + 历史周目静态快照原生缩放画布
    ├── chat/                # 全平台统一会话列表（MC/QQ/微信）、移动端气泡流、媒体灯箱、合并转发抽屉、独立官方交流群页
    ├── wiki/                # IGNGmc 离线百科目录（wiki.igng.net）与原生 Markdown 阅读器（含 TOC 抽屉）
    ├── player/              # 玩家个人控制中心（角色与身份核验、登录流水、处罚记录、仓库、领地、传送点、假人、统计、勋章、权益、权限）
    ├── ticket/              # 客服工单全生命周期、图片上传与预览、If-Match 并发保护、重开、本地草稿防丢失
    └── admin/               # 按角色动态解锁的口袋控制台（工单、审计、Historian 周报、流量与节点状态）
```

## 服务地址与端点约定

客户端所有第一方业务接口均统一经 `IGNG_API_BASE_URL`（`/api/v1/**`）访问，认证统一走 `IGNG_SSO_BASE_URL`；Bearer 令牌仅注入这两个受信任域名。

可通过 Gradle 属性覆写默认地址：

- **统一 API 网关与认证（唯一允许接口调用与令牌注入）**：
  - `IGNG_API_BASE_URL`：`https://www.igng.net`（全部第一方 Client API v1 接口与媒体代理）
  - `IGNG_SSO_BASE_URL`：`https://sso.igng.net`（OAuth 2.1 PKCE 授权与令牌轮换）
- **纯网页外链与地图 WebView 地址（不发送 API 请求或 Bearer 令牌）**：
  - `IGNG_MC_BASE_URL`：`https://mc.igng.net`（交互式世界地图 WebView 与历史快照图片）
  - `IGNG_DASH_BASE_URL`：`https://dash.igng.net`（外部浏览器跳转至网页端角色核验与管理台）
  - `IGNG_CHAT_BASE_URL`：`https://chat.igng.net`（复制单条聊天记录外链）

## 构建与单元测试

环境要求：JDK 21、Android SDK（`compileSdk 36`，`minSdk 26`）。

```bash
cd android
./gradlew assembleDebug testDebugUnitTest
```

Windows PowerShell：

```powershell
cd android
.\gradlew.bat assembleDebug testDebugUnitTest
```

如需覆盖服务端地址进行本地联调：

```bash
./gradlew assembleDebug -PIGNG_API_BASE_URL=https://www.igng.net -PIGNG_SSO_BASE_URL=https://sso.igng.net
```

## Release 签名

正式 Release 统一由 `.github/workflows/release.yml` 在 GitHub Actions 云端使用仓库 Secrets 完成签名与验签。如需在本地排查签名构建问题，仅通过环境变量（或 Gradle 属性）传入，不要在仓库内放置密钥文件：

- `ANDROID_SIGNING_STORE_FILE`
- `ANDROID_SIGNING_STORE_PASSWORD`
- `ANDROID_SIGNING_KEY_ALIAS`
- `ANDROID_SIGNING_KEY_PASSWORD`
- `IGNG_REQUIRE_RELEASE_SIGNING=true`（可选门控：缺签名凭据时直接失败，防止产出未签名 APK）
