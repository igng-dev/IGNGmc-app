# Contributing

## 首次配置钩子

- Windows（PowerShell）：

  ```powershell
  .\scripts\install-git-hooks.ps1
  ```

- Linux / macOS：

  ```bash
  git config core.hooksPath .githooks
  ```

  （若使用设备级 `~/.qoder/coder-rules/install-hooks.sh` 管理 `.git/hooks/`，请保持该脚本安装的钩子，无需额外切换 `core.hooksPath`。）

## 提交代码前

1. 从 `origin/main` 切出任务分支（如 `feat/*`、`fix/*`、`docs/*`、`agent/*`）。
2. 只暂存本次开发相关的文件：`git add <files>`。
3. 运行隐私扫描：
   - Linux / macOS：`bash scripts/check-privacy.sh`
   - Windows：`powershell -ExecutionPolicy Bypass -File .\scripts\check-privacy.ps1`
4. 运行 `git diff --cached --check` 检查空白字符和冲突标记。
5. 涉及 Android 代码改动时，确保 `cd android && ./gradlew assembleDebug testDebugUnitTest`（Windows：`.\gradlew.bat`）或 PR 上的 CI `verify` job 通过。
6. 确认暂存区没有账号、密码、令牌、私有地址、本地路径或真实用户数据。
7. 使用 Conventional Commits 格式，例如 `feat: add server status timeline` 或 `fix: restore ticket session`。

pre-commit hook 会自动执行隐私扫描和 `git diff --cached --check`。检查失败时不要使用 `--no-verify` 绕过 hook；先移除敏感内容。

## 推送与合并规则

- **禁止直接推送 `main`**；`main` 受分支保护与 `pre-push` 守卫约束。
- 统一流程：`git push -u origin <branch>` → 创建 Pull Request → 等待 CI（`verify`）通过并完成 review → Squash Merge。
- 禁止对 `main` 使用 `git push --force` 或改写受保护分支历史。
- 推送前确认 `git status` 和 `git diff --cached`，避免把其他本地工作一起提交。
- Release 使用新的递增版本号和 tag（例如 `v1.0.11`）；不要移动、删除或覆盖已经发布的 tag，也不要删除既有 Release 或重新上传同名资产，以保证历史版本下载链接持续有效。
