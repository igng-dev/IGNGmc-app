# IGNGmc Android

Jetpack Compose + Kotlin + Material 3 实现的 IGNG MC 服务器状态客户端。

## 已实现

- 顶部数字总览
- 按站点顺序展示全部服务器
- 每台服务器显示站点当前状态、TPS、MSPT、内存、CPU、在线人数、参考节点和丢包
- 首页按所选时间范围展示由服务端生成的状态时间线（覆盖、延迟、丢包语义与网页一致）
- 点击进入服务器详情
- 详情页展示 5 条折线图、当前状态、网络状态格和公开聚合流量
- 顶部时间粒度切换：`24h`、`3d`、`7d`、`30d`
- 状态页提供假人列表；管理员账户可进入受权限保护的 IP 流量明细
- 信息页提供站点信息大厅的公开领地和当前在线假人，搜索与详情展示适配移动端
- 聊天页支持服务器/审核 QQ 群切换、时间范围、发送者、@全体成员筛选
- 聊天页支持消息 ID 定位、复制、历史分页和自动刷新
- 设置页提供个人管理：Minecraft 账号、领地、传送点、登录记录、假人和只读权限

## 接口约定

默认对接 `https://mc.igng.net`，使用：

- `/api/status/list`
- `/api/status/nodes`
- `/api/status/current`
- `/api/status/timeline`
- `/api/status/server/{id}`
- `/api/status/traffic`
- `/api/status/fake-players`
- `/api/hall/lands`（仅返回站点标记为公开的领地）
- `/api/hall/fake-players`（仅返回最近 45 秒仍在同步的假人）
- `/api/auth/me`（登录后使用 Bearer 会话令牌判断管理员入口）
- `/api/status/traffic/admin`（管理员权限和 IP 明细由服务端校验）
- `/api/chatlogs/sources`
- `/api/chatlogs`
- `/api/mc/accounts`、`/api/mc/servers`
- `/api/mc/lands`、`/api/mc/teleports`、`/api/mc/logins`、`/api/mc/fake-players`
- `/api/mc/permissions`、`/api/mc/details`
- `/api/mc/manage`、`/api/mc/lands/manage`
- `https://docs.igng.net/api/wiki/catalog?kb=IGNGmc`
- `https://docs.igng.net/api/wiki/page?kb=IGNGmc&pageId=<id>`

教程页使用 `WIKI_BASE_URL` 对接 `docs.igng.net` 的公开 Wiki API。目录和文章会在打开或手动刷新时同步，网络不可用时显示最近一次缓存内容；文章正文不打包进 APK。

状态首页使用 `list` 获取服务器、`nodes` 获取节点信息，同时读取 `current` 获取站点权威状态，再使用 `timeline` 获取所选时间范围内的性能、延迟和服务端状态格；不再依赖站点保留的旧版 `/api/status/overview` 聚合接口。

为满足移动端首页卡片，网页端 `apps/mc/app/api/status/timeline/route.js` 已补充返回：

- `avg_mspt`
- `cpu_usage`
- `memory_usage_mb`

公开流量仅返回服务器级聚合值，不包含 IP；管理员流量接口通过 `Authorization: Bearer <session-token>` 复用 IGNG 会话，并由站点服务端执行角色校验。

聊天页使用 `/api/chatlogs/sources` 获取服务器和审核 QQ 群目录，再通过 `/api/chatlogs` 查询消息。服务器消息 ID 使用数字格式，QQ 消息 ID 使用 `qq-数字` 或 `qq-v-数字` 格式；QQ 历史分页游标必须沿用当前群组返回的 ID。聊天接口返回的 `moderation` 是对象（包含 `level` 和 `label`），客户端不再调用已不存在的举报和举报额度接口。

## 构建

```powershell
cd android
.\gradlew.bat assembleDebug
```

## 自定义接口地址

可通过 Gradle 属性覆盖：

```powershell
.\gradlew.bat assembleDebug -PMC_STATUS_BASE_URL=https://your-host
```
