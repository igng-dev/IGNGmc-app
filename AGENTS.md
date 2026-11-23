# IGNGmc应用（igng-dev/IGNGmc-app）· 项目 Agent 规范

只写本仓库的领域边界、接口契约与仓库特有规则；通用 Git 纪律、worktree、冲突处理与授权语义见设备级规范 `~/.qoder/coder-rules/global-rules.md`。`CLAUDE.md` 仅作 `@AGENTS.md` 薄指针，改约定只改本文件，不要双写。

Collaboration: collaborative
Baseline: main
Default branch: main
Integration: pull-request
Release: cloud-workflow / tag-increment-only（经 `.github/workflows/release.yml` 在 GitHub Actions 云端使用仓库 Secrets 完成签名构建、证书指纹验签与 Release 发布；推送 `v*.*.*` tag 或手动 `workflow_dispatch` 触发，不随 PR 合并自动发生）
Validation: 本地 `bash scripts/check-privacy.sh`（Windows：`powershell -ExecutionPolicy Bypass -File .\scripts\check-privacy.ps1`）、`git diff --cached --check`；Android 构建与单测（`assembleDebug` / 未签名 `assembleRelease` / `testDebugUnitTest` / 版本号一致性）由 PR 上的 CI（`.github/workflows/ci.yml` 的 `verify` job）执行，内循环调试可直接在本地 worktree 工作区执行
Worktree: `~/dev/.wt/IGNGmc应用/<slug>`

## 项目范围

- IGNGmc 官方原生 Android 客户端，正式包名 `net.igng.mcstatus`，最低版本 `minSdk = 26`，工程入口在 `android/`（Kotlin + Jetpack Compose 14 模块架构，Gradle 要求 JDK 21）。
- 对接 IGNG 统一客户端网关（`https://www.igng.net/api/v1/**`）与官方 OAuth 2.1 PKCE 认证中心（`https://sso.igng.net`）。工单、角色、领地、仓库、聊天与百科的字段、权限、可见性、状态规则**以服务端 v1 契约为准**（同级仓库 `~/dev/IGNG/IGNG站点/docs/internal-api-v1.md` 与 `contracts/client-api-v1.openapi.json`），不得在客户端复制或改写业务规则。
- 默认服务地址写在公开 Gradle 配置里；不得把测试地址、内网地址、本机个人路径、账号或凭据写进源码。

## Client API v1 契约与跨仓联动规则（强制）

- **唯一 API 网关原则**：客户端**只能**调用 `https://www.igng.net/api/v1/**`（及 `https://sso.igng.net/oauth/authorize`、`POST /api/oauth/token`）；**禁止**在 `Repositories.kt` 或任何网络层直接请求 `mc.igng.net/api/**`、`dash.igng.net/api/**`、`chat.igng.net/api/**`、`wiki.igng.net/api/**` 或 `sso.igng.net/api/mobile/**`（WebView 地图与外链浏览器跳转除外）。
- **认证与请求头契约**：
  - 所有 `/api/v1/**` 请求必须携带 `X-Client-Id: igng_mc_android`、`X-Client-Version`、`X-Device-Id`、`User-Agent: IGNGmc-Android/<version>`；所有 `/api/v1/**` `POST` 请求必须携带 `Idempotency-Key`；`PATCH /api/v1/tickets/{ticket_id}` 必须携带 `If-Match`。
  - Bearer 令牌仅注入 `www.igng.net` 与 `sso.igng.net`；`refresh_token` 单次使用且轮换，并发 401 刷新必须经过 `SingleFlightTokenRefresher` 互斥锁，禁止并发重放旧 `refresh_token`。
- **契约先行与黄金样本同步**：
  - 每次需要新增或调整服务端接口时，必须先在 `IGNG站点` 更新 `docs/internal-api-v1.md`、生成 `contracts/client-api-v1.openapi.json` 与 `contracts/fixtures/igngmc-v1/*.json`，再把最新黄金样本同步到 `android/core/data/src/test/resources/fixtures/v1/` 并保证 `DtoDeserializationTest` 全部通过。
  - PR 描述涉及接口变更时，必须在正文中标注对应的 `igng-dev/igng-sites` PR 链接与契约版本号。

## 隐私与安全（永不进入 Git / GitHub）

- `local.properties`、`.env` / `.env.*`（`.env.example` 除外）、本机个人绝对路径（`/Users/<user>/`、`/home/<user>/`、`C:\Users\<user>\`）与内网私网地址
- 任何签名密钥、口令与凭据文件（`android/keystore/`、`signing/`、`keystore.properties`、`release-signing.properties`、`.jks`、`.keystore`、`.p12`、`.pfx`、`.pem`、`.key`、`.der`）
- 账号密码、会话令牌、API key、SSH 私钥、Cookie、验证码、真实用户数据
- 构建产物、`artifacts/`、本地 APK / AAB 及含隐私的日志、截图、调试转储、临时文件

提交前必须扫描并确认通过：
- Linux / macOS：`bash scripts/check-privacy.sh`（全仓审计：`bash scripts/check-privacy.sh --all`）
- Windows：`powershell -ExecutionPolicy Bypass -File .\scripts\check-privacy.ps1`（全仓审计加 `-All`）
- 空白与冲突标记：`git diff --cached --check`

扫描失败先清除敏感内容，不得绕过 hook（`--no-verify`）或 `git add -f` 被忽略文件；疑似泄露必须在最终回复中明确说明。

## 构建、验证与云端 Release

- **默认以 PR 上的 CI（`.github/workflows/ci.yml` 的 `verify` job）为准**：涵盖全仓隐私扫描（`check-privacy.sh --all`）、脚本语法检查、版本号一致性检查（`android/app/build.gradle.kts` 的 `versionName` 与 `IgngHttpClientFactory.DEFAULT_CLIENT_VERSION`）、`assembleDebug`、未签名 `assembleRelease`、`testDebugUnitTest` 与调试 APK artifact；纯 `*.md` 文档改动自动跳过 Gradle 重构建。引用 CI 结果必须附 run 链接与 job 名。
- **云端 Release 发布（`.github/workflows/release.yml`）**：
  - 签名密钥与期望证书指纹统一存放在仓库 GitHub Actions Secrets：`ANDROID_KEYSTORE_BASE64`、`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD`、`ANDROID_SIGNING_CERT_SHA256`。
  - **正式发版一律走云端工作流**，不得把签名密钥放进仓库工作区手工打包。流程：先通过 PR 同步递增 `android/app/build.gradle.kts` 的 `versionCode`/`versionName` 与 `IgngHttpClientFactory.DEFAULT_CLIENT_VERSION` → 合并进 `main` → 推送对应 `vX.Y.Z` tag 或手动触发 `Release` 工作流（支持 `dry_run=true` 仅做云端签名构建与验签演练）。
  - 云端工作流自动执行：全仓隐私扫描 → 单元测试 → `IGNG_REQUIRE_RELEASE_SIGNING=true` 正式签名 `:app:assembleRelease` → `apksigner verify --verbose --print-certs` 并强校验签名证书 SHA-256 → `aapt2 dump badging` 校验包名与版本号 → 发布 `IGNGmc-vX.Y.Z.apk` 与 `.sha256`。
- **本地开发与内循环调试**：
  - 直接在本地任务 worktree 内运行 `cd android && ./gradlew assembleDebug testDebugUnitTest`（Windows：`.\gradlew.bat`），共享缓存复用本机 `GRADLE_USER_HOME`（默认 `$BUILD_SCRATCH_ROOT` 下），不提交构建产物。
  - 若因排查问题在本地执行签名 Release 构建，仅通过环境变量注入 `ANDROID_SIGNING_STORE_FILE` / `ANDROID_SIGNING_STORE_PASSWORD` / `ANDROID_SIGNING_KEY_ALIAS` / `ANDROID_SIGNING_KEY_PASSWORD`，不得在仓库内创建 `keystore/` 或 `keystore.properties`。
- 失败的测试、构建警告、未执行的验证必须如实报告，不得声称已验证。

## 分支与推送规范

1. 只暂存本任务相关文件；使用 Conventional Commits（`feat:` `fix:` `docs:` `test:` `chore:` `refactor:`）。
2. **本仓库是 `igng-dev` 组织多人协作公开仓库，`main` 受 GitHub 分支保护（要求 `verify` 通过）与本地 `pre-push` 守卫双重约束：严禁直接推送 `main`**。统一集成出口：`git push -u origin <branch>` → `gh pr create` → CI（`verify`）通过 + review → Squash Merge → `wt done IGNGmc应用 <slug>`。
3. 禁止对 `main` 使用 `git push --force` 或改写受保护分支历史；Agent 独占任务分支仅在必要时允许 `--force-with-lease` 并说明原因。
4. 钩子说明：Linux/macOS 若由设备级 `~/.qoder/coder-rules/install-hooks.sh` 管理 `.git/hooks/`，其 `pre-commit` 负责拦截 `_anchor` 主目录提交，`pre-push` 负责拦截直推受保护分支，提交前需手工执行 `bash scripts/check-privacy.sh` 与 `git diff --cached --check`；Windows / 独立克隆可运行 `.\scripts\install-git-hooks.ps1`（或 `git config core.hooksPath .githooks`）启用版本化 `.githooks/`。

## Release 保护

- 所有已发布的 tag、Release 与 APK 下载地址**必须保持不变**：不得移动、删除、覆盖旧 tag，不得删除已有 Release 或重新上传同名资产。
- 后续发布只能使用新的递增版本号与 `vX.Y.Z` tag，新 APK 按 `IGNGmc-vX.Y.Z.apk` 命名并附带 `.sha256`。
- 只改文档、规则或普通代码时，不得重新构建或替换旧 Release 资产。
- Agent 不得自行创建 Release 或打 tag，除非用户明确要求。
