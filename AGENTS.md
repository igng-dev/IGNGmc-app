# IGNGmc软件（igng-dev/IGNGmc-app）· 项目 Agent 规范

本文件适用于所有编码 Agent（Codex / Qoder / Claude / Gemini / opencode），会随仓库带到 Windows 与 Android 开发机。
只写本仓库与设备级规范的差异；Git 纪律、worktree、冲突处理见 `~/.qoder/coder-rules/global-rules.md`。

Collaboration: collaborative
Default branch: main
Integration: pull-request
Release: tag-increment-only
Worktree: `~/项目/.wt/IGNGmc软件/<slug>`

## 项目范围

- Kotlin + Jetpack Compose 的 Android 应用，工程入口在 `android/`。
- 对接 IGNG 的服务器状态与工单服务。工单的字段、权限、可见性、状态规则**以服务端接口为准**，不得在客户端复制或改写业务规则。
- 默认服务地址写在公开构建配置里；不得把测试地址、内网地址、账号或凭据写进源码。

## 开始工作前

1. `git status --short --branch`，阅读相关源码与已有改动。
2. 保留用户已有的未提交修改；不得使用 `git reset --hard`、`git checkout --` 等破坏性命令。
3. 改前确认任务涉及的模块、接口契约与页面；不为完成小功能重构无关代码。
4. 新增抽象前先确认没有可复用模式。

## 隐私与安全（永不进入 Git / GitHub）

- `local.properties`、`.env`、真实配置文件、本机绝对路径
- `android/keystore/`、`.jks`、`.keystore`、`.p12`、`.pfx` 及口令文件
- 账号密码、会话令牌、API key、SSH 私钥、Cookie、验证码、真实用户数据
- 含隐私的日志、截图、调试转储、临时文件
- 未经明确要求的构建产物或本地 APK

改完必须扫描：Linux/macOS `bash scripts/check-privacy.sh`；Windows `powershell -ExecutionPolicy Bypass -File .\scripts\check-privacy.ps1`；然后 `git diff --cached --check`。扫描失败先清除敏感内容，不得绕过 hook 或 `git add -f` 被忽略文件；疑似泄露必须在最终回复中明确说明。

## 构建与验证

- 普通代码改动至少：`cd android && ./gradlew assembleDebug`（Windows：`cd android; .\gradlew.bat assembleDebug`）。
- 影响 Release、Manifest、依赖或签名配置：再跑 `assembleRelease`。
- Release APK 必须用 `apksigner verify --verbose --print-certs` 验证签名，不得用 `jarsigner` 或"Gradle 成功"代替。
- 失败的测试、构建警告、未执行的验证必须如实报告，不得声称已验证。
- 改动依赖服务端接口时，优先核对 IGNG 站点契约与真实响应。
- **本机内存 8G、且本仓库在 NAS 共享上**：Gradle 构建优先在 worktree 内并复用共享 `GRADLE_USER_HOME`，不要把构建产物留在多个 worktree 里各存一份。

## 已知缺口

- **本仓库没有任何 `.github/workflows/`**，所以"PR 必须过 CI"当前不成立。先补一个最小 verify workflow（隐私扫描 + assembleDebug + 产物 artifact），且单独一个 PR，不夹带功能改动。

## 提交与推送

1. 只暂存本任务相关文件；Conventional Commits（`feat:` `fix:` `docs:` `test:` `chore:`）。
2. 提交前再查 `git diff --cached --name-only` 与隐私扫描结果。
3. **本仓库是 `igng-dev` 多人仓库：禁止直接推送 `main`**（这是对旧版"正常推送到 main"的有意收紧）。改为：`git push -u origin <branch>` → 创建 PR → CI 与人 review → Squash Merge。
4. 禁止 `git push --force`、禁止改写远程历史。
5. 推送后核验远程分支、提交 hash、工作区状态并在回复中报告。

## Release 保护

- 已发布的 `v1.0.0` 等 tag、Release 与 APK 下载地址**必须保持不变**：不得移动、删除、覆盖旧 tag，不得删除已有 Release 或重新上传同名资产。
- 后续发布只能用新的递增版本号与 tag（如 `v1.0.1`），新 APK 用新文件名与新版本号。
- 发布前必须完成：Release 构建 → 签名验证 → SHA-256 计算 → 隐私扫描。
- 只改 README、规则或普通代码时，不得重新构建或替换旧 Release 资产。
- Agent 不得自行创建 Release 或打 tag，除非用户明确要求。

## 最终报告

说明：改动文件、执行过哪些验证及其结果、提交 hash、推送分支、是否新建 Release、worktree 是否清理。回复/日志/提交信息中不得输出密钥、密码或完整令牌。
