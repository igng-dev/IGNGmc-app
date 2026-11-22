# IGNGmc应用（igng-dev/IGNGmc-app）· 项目 Agent 规范

本文件适用于所有编码 Agent（Codex / Qoder / Claude / Gemini / opencode），会随仓库带到 Windows 与 Android 开发机。
> 设备级规范：`~/.qoder/coder-rules/global-rules.md`（本机所有 harness 已引入）。以下 7 行是模式判定，正文为仓库自有约定；冲突时安全条款以设备级为准。`CLAUDE.md` 仅作 `@AGENTS.md` 薄指针，改约定只改本文件，不要双写。

Collaboration: collaborative
Baseline: main
Default branch: main
Integration: pull-request
Release: tag-increment-only（发版由用户明确要求后执行，不随 PR 合并自动发生）
Validation: 本地 `bash scripts/check-privacy.sh`（Windows：`powershell -ExecutionPolicy Bypass -File .\scripts\check-privacy.ps1`）、`git diff --cached --check`；Android 构建与单测（`assembleDebug` / `testDebugUnitTest`）优先由 PR 上的 CI（`.github/workflows/ci.yml` 的 `verify` job）执行，排查构建问题或 CI 不可用时再在本机 `$BUILD_SCRATCH_ROOT` / worktree 执行
Worktree: `~/项目/.wt/IGNGmc应用/<slug>`

## 项目范围

- Kotlin + Jetpack Compose 的 Android 应用，工程入口在 `android/`。
- 对接 IGNG 的服务器状态与工单服务。工单的字段、权限、可见性、状态规则**以服务端接口为准**（同级目录 `~/项目/IGNG/IGNG站点`），不得在客户端复制或改写业务规则。
- 默认服务地址写在公开构建配置里；不得把测试地址、内网地址、账号或凭据写进源码。

## 开始工作前

1. 遵守设备级开工三步：`git fetch origin --prune` → 解析远端默认分支 → `wt new IGNGmc应用 <slug>`，只在 `~/项目/.wt/IGNGmc应用/<slug>/` 内开发；主目录停在 `_anchor` 空树分支，禁止在主目录切分支或提交。
2. `git status --short --branch`，阅读相关源码与已有改动。
3. 保留用户已有的未提交修改；不得使用 `git reset --hard`、`git checkout --` 等破坏性命令。
4. 改前确认任务涉及的模块、接口契约与页面；不为完成小功能重构无关代码。
5. 新增抽象前先确认没有可复用模式。

## 隐私与安全（永不进入 Git / GitHub）

- `local.properties`、`.env` / `.env.*`（`.env.example` 除外）、真实配置文件、本机绝对路径
- `android/keystore/`、`keystore.properties`、`.jks`、`.keystore`、`.p12`、`.pfx`、`.pem`、`.key`、`.der` 及口令文件
- 账号密码、会话令牌、API key、SSH 私钥、Cookie、验证码、真实用户数据
- 含隐私的日志、截图、调试转储、临时文件
- 未经明确要求的构建产物或本地 APK / AAB

改完必须扫描：
- Linux / macOS：`bash scripts/check-privacy.sh`（全仓审计可用 `bash scripts/check-privacy.sh --all`）
- Windows：`powershell -ExecutionPolicy Bypass -File .\scripts\check-privacy.ps1`（全仓审计加 `-All`）
- 空白与冲突标记：`git diff --cached --check`

扫描失败先清除敏感内容，不得绕过 hook（`--no-verify`）或 `git add -f` 被忽略文件；疑似泄露必须在最终回复中明确说明。

## 构建与验证

- **默认以 PR 上的 CI（`.github/workflows/ci.yml` 的 `verify` job）为准**：涵盖全仓隐私扫描（`check-privacy.sh --all`）、`assembleDebug`、`testDebugUnitTest` 与调试 APK artifact。引用 CI 结果必须附 run 链接与 job 名。
- **本地验证**（仅在排查构建问题、CI 不可用或内循环调试时）：
  - 普通代码改动至少：`cd android && ./gradlew assembleDebug` 与 `./gradlew testDebugUnitTest`（Windows：`.\gradlew.bat`）。
  - 影响 Release、Manifest、依赖或签名配置：再跑 `assembleRelease`。
  - Release APK 必须用 `apksigner verify --verbose --print-certs` 验证签名，不得用 `jarsigner` 或"Gradle 成功"代替。
  - 遵循设备级规范第 5c / 8 节：复用本机盘 `GRADLE_USER_HOME`（落在 `$BUILD_SCRATCH_ROOT` 下），不把构建产物留在多个 worktree 或提交进仓库。
- 仅文档改动（diff 仅含 `*.md`）按设备级规范第 5 节执行范围校验、文档结构检查与凭据扫描，并说明未跑项理由。
- 失败的测试、构建警告、未执行的验证必须如实报告，不得声称已验证。
- 改动依赖服务端接口时，优先核对 IGNG 站点契约与真实响应。

## 分支与推送规范

1. 只暂存本任务相关文件；Conventional Commits（`feat:` `fix:` `docs:` `test:` `chore:` `refactor:`）。
2. 提交前再查 `git diff --cached --name-only`、`git diff --cached --check` 与隐私扫描结果。
3. **本仓库是 `igng-dev` 组织多人协作公开仓库，`main` 受 GitHub 分支保护与本地 `pre-push` 守卫双重约束：严禁直接推送 `main`**。统一集成出口：`git push -u origin <branch>` → `gh pr create` → CI（`verify`）通过 + review → Squash Merge → `wt done IGNGmc应用 <slug>`。
4. 禁止对 `main` 使用 `git push --force` 或改写受保护分支历史；Agent 独占任务分支仅在必要时允许 `--force-with-lease` 并说明原因。
5. 本机钩子：Linux/macOS 由设备级 `~/.qoder/coder-rules/install-hooks.sh` 安装（含主目录 `pre-commit` 与 `pre-push`）；Windows / 独立克隆可运行 `.\scripts\install-git-hooks.ps1`（或 `git config core.hooksPath .githooks`）启用版本化 `.githooks/`。
6. 推送后核验远程分支、提交 hash、工作区状态并在回复中报告。

## Release 保护

- 已发布的 `v1.0.0` 等 tag、Release 与 APK 下载地址**必须保持不变**：不得移动、删除、覆盖旧 tag，不得删除已有 Release 或重新上传同名资产。
- 后续发布只能用新的递增版本号与 tag（如 `v1.0.11`），新 APK 用新文件名与新版本号。
- 发布前必须完成：Release 构建 → 签名验证 → SHA-256 计算 → 隐私扫描。
- 只改 README、规则或普通代码时，不得重新构建或替换旧 Release 资产。
- Agent 不得自行创建 Release 或打 tag，除非用户明确要求。

## 最终报告

说明：分支名、worktree 路径、改动文件、执行过哪些验证（含 CI run 链接与耗时）及其结果、提交 hash、PR 链接与是否已合并、是否新建 Release、以及 `wt done` 清理结果。回复/日志/提交信息中不得输出密钥、密码或完整令牌。
