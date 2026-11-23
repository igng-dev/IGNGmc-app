# Contributing

## 首次配置钩子

- Windows（PowerShell）：

  ```powershell
  .\scripts\install-git-hooks.ps1
  ```

- Linux / macOS（独立克隆）：

  ```bash
  git config core.hooksPath .githooks
  ```

  启用 `.githooks/` 后，`pre-commit` 会自动执行隐私扫描与 `git diff --cached --check`，`pre-push` 会拦截直推受保护分支。

  > 若本机使用设备级 `~/.qoder/coder-rules/install-hooks.sh` 管理 `.git/hooks/`，请保持该脚本安装的钩子，无需额外切换 `core.hooksPath`（设备级 `.git/hooks/pre-commit` 负责保护 `_anchor` 主目录，`pre-push` 负责拦截直推受保护分支；提交前请按下方步骤手工执行隐私扫描与空白检查）。

## 提交代码前

1. 从 `origin/main` 切出任务分支（如 `feat/*`、`fix/*`、`docs/*`、`agent/*`）。
2. 只暂存本次开发相关的文件：`git add <files>`。
3. 运行隐私扫描：
   - Linux / macOS：`bash scripts/check-privacy.sh`（全仓审计：`bash scripts/check-privacy.sh --all`）
   - Windows：`powershell -ExecutionPolicy Bypass -File .\scripts\check-privacy.ps1`（全仓审计加 `-All`）
4. 运行 `git diff --cached --check` 检查空白字符和冲突标记。
5. 涉及 Android 代码改动时，确保本地 `cd android && ./gradlew assembleDebug testDebugUnitTest`（Windows：`.\gradlew.bat`）或 PR 上的 CI `verify` job（含 `assembleDebug`、未签名 `assembleRelease` 与单测）通过。
6. 确认暂存区没有签名密钥、口令、令牌、私网 IP、本机个人路径或真实用户数据。
7. 使用 Conventional Commits 格式，例如 `feat: add server status timeline` 或 `fix: restore ticket session`。

隐私扫描或钩子检查失败时，请先移除敏感内容，不要使用 `--no-verify` 绕过检查。

## 推送与合并规则

- **禁止直接推送 `main`**；`main` 受 GitHub 分支保护（要求 `verify` 通过）与本地 `pre-push` 守卫约束。
- 统一流程：`git push -u origin <branch>` → 创建 Pull Request → 等待 CI（`verify`）通过并完成 review → Squash Merge。
- 禁止对 `main` 使用 `git push --force` 或改写受保护分支历史。
- 推送前确认 `git status` 和 `git diff --cached`，避免把其他本地工作一起提交。

## 发布规则

- 正式发布统一经由 `.github/workflows/release.yml` 在 GitHub Actions 云端使用仓库 Secrets 签名构建并创建 Release，不在本地放置密钥文件手工打包。
- 发版前先通过 PR 同步递增 `android/app/build.gradle.kts` 的 `versionCode` / `versionName` 与 `IgngHttpClientFactory.DEFAULT_CLIENT_VERSION`，合并后再推送对应递增 `vX.Y.Z` tag 或手动触发 `Release` 工作流。
- 严禁移动、删除或覆盖已经发布的 tag，也不要删除既有 Release 或重新上传同名资产，以保证历史版本下载链接持续有效。
