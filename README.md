# IGNGmc

IGNGmc 是面向 IGNG Minecraft 玩家的官方原生 Android 客户端（Kotlin + Jetpack Compose），基于 IGNG Bento 2.0 设计语言与折角核心动效体系构建，统一对接 IGNG 第一方客户端网关（`https://www.igng.net/api/v1/**`）与官方 OAuth 2.1 PKCE 认证中心（`https://sso.igng.net`）。

- 正式包名：`net.igng.mcstatus`（支持 Android 8.0 / `minSdk = 26` 及以上版本，支持从历史版本平滑覆盖升级）
- 项目仓库：[igng-dev/IGNGmc-app](https://github.com/igng-dev/IGNGmc-app)
- Release 下载：[Releases](https://github.com/igng-dev/IGNGmc-app/releases)

## 你可以用它做什么

- **探索与服务器家族**：查看纯净服、轻量玩法服、MOD 周目服、作弊服与档案馆的存活周目与历史档案，支持插件外链、世界入口、一键复制连接地址与种子
- **实时运行状态墙**：查看全服在线状态、在线/上限玩家数、TPS、延迟趋势曲线与健康记录时间轴（支持手势拖动精确读数）
- **交互式世界地图**：浏览存活周目与归档周目的交互式 Conflux 地图，以及已关停周目的高清历史快照地图
- **全站玩家搜索与社区大厅**：检索玩家通行证与 MC 角色档案，浏览公开领地、在线假人、公开工单与完美之人荣誉墙
- **全平台跨服与社交聊天记录**：合并 MC 服务器、QQ 群聊/私聊与微信群聊/私聊的统一会话列表与移动端气泡流，支持平台筛选、搜索、多图/动图灯箱、合并转发多层展开、语音/视频/文件缓存、云樱 AI 溯源与独立「官方交流群与社区」页
- **IGNGmc 官方百科**：离线缓存 `wiki.igng.net` 文档，支持全文标题搜索、最近阅读、代码一键复制与文章目录大纲
- **完整玩家控制中心**：
  - **我的角色**：绑定与游戏内 `/igng verify` 身份核验状态、公开/私密切换、Step-up 密码信封修改 AuthMe 游戏密码、生效处罚与历史处罚记录回溯
  - **登录与活动**：按已核验角色回溯 Minecraft 游戏内登录流水（服务器、时间、状态、IP、世界与坐标）
  - **我的工单**：发起、编辑、回复、结单与重新打开，支持图片附件上传/浏览、`If-Match` 并发保护及本机草稿自动保存
  - **跨服物资仓库（OneWayVault）**：物品检索、NBT 附魔与 Lore 查看、存取流水与流转追踪
  - **世界与建造**：我的领地（含在线编辑描述与公开开关）、我的传送点坐标复制、我的假人状态
  - **身份与荣誉**：Minecraft 原版游戏统计可视化、我的勋章（含自定义称号在线改名）、完美之人权益额度、LuckPerms 游戏权限与到期洞察
  - **每日经验自动结算**：登录后自动触发每日登录经验结算
- **管理员口袋控制台**（仅对 `ADMIN / TECHADMIN / SUPERADMIN` 动态解锁）：全站工单快速处置、玩家审计与处罚记录、Historian 服务器观察周报、实时节点状态与网络流量审计

## 安全与登录

- 采用 **OAuth 2.1 + PKCE（S256）** 通过系统浏览器（Chrome Custom Tabs）向 `sso.igng.net` 完成授权，客户端不接触明文账号密码；若手机浏览器已登录任意 IGNG 站点可一键完成授权
- 支持多账号安全切换与单次轮换 `refresh_token` 自动续签，令牌由 Android Keystore（`AES/GCM/NoPadding`）硬件加密保存在本机（已通过备份规则排除云备份与换机迁移）
- 修改游戏密码使用与网页端一致的 RSA-OAEP-SHA256 + AES-256-GCM 密码信封
- 请不要在公开 issue 或截图中分享密码、验证码与会话令牌

## 下载

前往 [Releases](https://github.com/igng-dev/IGNGmc-app/releases) 下载最新版本（历史版本如 [v1.0.10](https://github.com/igng-dev/IGNGmc-app/releases/tag/v1.0.10) 保持原样不变）。

所有正式版 APK 均通过 GitHub Actions 云端签名构建，附带 SHA-256 校验文件与签名证书指纹，可直接覆盖升级。

## 开发、贡献与安全反馈

- Android 工程架构与构建说明见 [android/README.md](android/README.md)
- 贡献流程与钩子配置见 [CONTRIBUTING.md](CONTRIBUTING.md)
- 安全漏洞与敏感信息反馈方式见 [SECURITY.md](SECURITY.md)

## 开源许可

本项目以 MIT License 开源，详见 [LICENSE](LICENSE)。
