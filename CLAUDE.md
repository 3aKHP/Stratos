---
project: "Stratos (GPS-Plane)"
branch: "main"
---

# CLAUDE.md — AI 协作者说明

Stratos 是一款民航窗座场景的 GPS 飞行仪表 Android 应用。本文件记录**每次会话必读**的工作流。

## 相关文档

| 想看... | 去哪里 |
|---|---|
| 项目现状、版本状态、仓库结构、遗留 TODO | [`STATUS.md`](STATUS.md) |
| 代码架构、反模式、版本同步清单、已知陷阱、测试规范 | [`docs/developer/style.md`](docs/developer/style.md) |
| 外部贡献者英文版规范 | [`CONTRIBUTING.md`](CONTRIBUTING.md) |
| 本地草稿/秘密 | `local_dev/`（已忽略） |

动手写代码前先读 `docs/developer/style.md`；不确定项目欠什么时查 `STATUS.md`。

---

## 启动准则

三条硬规则：

- **需明确指令才 Commit**。对话里讨论到"要提交"不算指令，必须出现"请提交 / 请 commit / 请开 PR"这类明确祈使句
- **一般不在 main 直接工作**，但**允许例外**：大 PR merge 完后的 chore/docs 级小修补（补一个版本号遗漏、改 typo）可以直接在 main 上。feat/fix/refactor 级一律走分支
- **不主动 push**。即使刚 commit 完，也等用户说"请推"

## 分支命名

`<type>/v<version>-<topic>`，type 用 Conventional Commits 的类型。例：
- `chore/v0.1.1-housekeeping`
- `feat/v0.2.0-alpha.1-sensors`

## 单次迭代循环

一个大修改（从"你决定要做 X"到"main 合进 X"）的标准循环：

1. **对齐计划**：动手前用 1-2 段话描述打算做什么、拆成几个 commit、可能的风险。等用户点头
2. **拉分支**：按上面的命名约定
3. **动手**：按 commit 主题分批提交，每个中间 commit 都能独立编译（bisect-friendly）
4. **本地验证**：`./gradlew test assembleDebug` 全绿
5. **推分支 + 开 PR**：PR body 包含 Summary / Test plan / 未尽事宜三段
6. **独立 CR**：spawn 子代理做独立 review（见下文）
7. **应对 CR**：blocking 和 should-fix 处理掉，推到同分支；nits 酌情
8. **真机验证**：涉及 UI / 权限 / sensor / 存储路径 / lifecycle 的改动必须在真机过一遍
9. **人类 merge**：Claude 不做 merge，等用户确认
10. **本地清扫**：`git checkout main && git pull && git branch -d <branch> && git remote prune origin`

## Commit 规范

严格遵守 [Conventional Commits](https://www.conventionalcommits.org/)。

格式：`<type>(<scope>): <subject>`

- **type**：`feat` / `fix` / `refactor` / `docs` / `chore` / `test` / `style` / `perf`
- **scope** 常用：`data` / `sensors` / `tiles` / `ui` / `lifecycle` / `release` / `ci`
- **subject** 小写、祈使、≤72 字符、无句号

多行 body 用 HEREDOC：

```bash
git commit -m "$(cat <<'EOF'
feat(sensors): add load factor, turn rate, and barometer

Detailed explanation...
EOF
)"
```

不使用 `--amend`（除非用户明确要求）；pre-commit hook 失败时不加 `--no-verify`。

## 独立 CR 规范

**每个 PR 都应被一个独立子代理审阅一次**——子代理看不到我们的讨论过程，从 code-only 视角会发现我们共同忽略的东西。

**调用方式**：spawn 一个 `general-purpose` 子代理，prompt 要点：
- 明确说明审阅者视角独立、要 critical
- 提供 PR URL、分支名、基于的主线
- 列出 PR 自述（代理不看 PR 描述会默认相信提交信息）
- 给具体的审查清单（物理/数学/生命周期/版本一致性/测试质量/CHANGELOG 等）
- 要求结构化输出：**Blocking / Should-fix / Nits / Verified claims**

**CR 返回后的处理**：
- Blocking 必修；Should-fix 原则上都做，除非有充分理由推迟
- 修完推到同分支，给 Codex / 评论者明确回复
- 涉及架构决策的分歧（例如"要不要合进本 PR"）先同步用户再动

**历史教训**：Codex 曾抓到子代理漏掉的 `combine` 首发阻塞。遇到涉及 flow 组合的代码，CR prompt 里显式要求"模拟调用链的时间序列"。

## 真机验证清单

不是每个 PR 都要走真机，但以下任一情况必须先真机后 merge：

- 改了 UI（新增/调整 Composable、涉及 layout 尺寸）
- 改了权限 Manifest
- 新增了 sensor / LocationManager 订阅
- 改了 `noBackupFilesDir` / `filesDir` / `cacheDir` 这类存储路径
- 改了 lifecycle 相关逻辑

debug APK 在 `app/build/outputs/apk/debug/app-debug.apk`，Android Studio 直接 Run。
