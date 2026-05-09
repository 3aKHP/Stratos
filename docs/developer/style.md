# docs/developer/style.md — 代码规范与架构

面向所有协作者（人类与 AI）。本文件记录 Stratos 的代码架构硬原则、反模式、版本同步清单、CHANGELOG 规则、测试规范以及历史陷阱。

日常工作流见 [`../../CLAUDE.md`](../../CLAUDE.md)；项目现状见 [`../../STATUS.md`](../../STATUS.md)；外部贡献者英文版见 [`../../CONTRIBUTING.md`](../../CONTRIBUTING.md)。

---

## 代码规范与架构

**项目维护者对代码架构和模块化解耦要求很高**。上帝文件和面条代码是底线问题，在它们出现之前就要阻止。以下是硬性原则，不是"nice to have"。

### 文件大小与职责

- **单一职责**：一个文件只干一件事。`GpsRepository` 只负责 GPS 数据订阅，不混进姿态；`PressureMath` 只管气压↔高度换算，不放单位换算
- **文件长度预警线**：Kotlin 源文件超过 ~300 行就要问"这能不能拆"。`GpsScreen.kt` 目前接近 700 行（Composable 树 + 格式化 + 枚举 + 子组件全在一个文件），**是下一个需要拆分的目标**——新 UI 功能不该继续往里堆
- **Repository 只做数据订阅**：纯数学/滤波/统计**必须**抽到独立 `object` 或 `class`（参考 `VerticalSpeedFilter`、`AttitudeMath`、`SatelliteStats`、`PressureMath`、`EmaFilter`）。Repository 里只剩 `callbackFlow` 的注册/反注册骨架和对纯函数的调用

### 分层纪律

```
ui/screen/       ←  Composable，接收 data model，无业务逻辑
ui/theme/        ←  Material 3 主题定义
data/            ←  Repository（callback → Flow） + 纯函数 helper
data/model/      ←  Plain data class，无方法
data/tiles/      ←  osmdroid 相关适配
util/            ←  跨领域纯函数（单位换算等）
```

**允许的依赖方向**：`ui → data → data/model`。**禁止**：Composable 里直接调 `LocationManager` / `SensorManager`；`data/model` 里写计算逻辑；`util` 依赖 `data`。

### 抽取的触发条件

遇到以下任一情况**立即**抽成独立单元，不要等下次 PR：

- 同一个数学公式在 ≥2 个地方出现（例：hPa→inHg 曾在两个 BaroCell 里重复，已抽到 `UnitConverter.hpaToInHg`）
- 一个函数/Composable 超过 ~50 行或嵌套超过 3 层
- 一段逻辑有明显的"状态 + 更新 + 查询"三要素（→ 独立类）
- 一段逻辑需要单独测试（→ 独立纯函数/object）

### 模块化 vs 过度抽象

不要为了抽而抽。**单次使用、少于 10 行、语义清晰**的内联代码不需要抽。判断基准："如果我明天给这块代码写单测或者重用它，现在的形状会让我想重写吗？"——会就抽，不会就留着。

### 命名与样式

- Kotlin 官方编码规范（`kotlin.code.style=official` 已设）
- 公开 API（`public` / 默认可见性）必须有 KDoc，说明 **what + why**，不说 **how**
- 纯函数优先放在 `object` 里；有状态的放 `class`
- 不写"废话注释"（`// increment i by 1`）；非显而易见的约束、反直觉的工作区（例如 `turnRate` 的 sign flip）必须注释
- 数据类保持 data-only——不在 `data class` 里写方法，相关的纯函数放到同目录的 `object`

### UI 代码专项

Compose 层特别容易变成面条。额外要求：

- **顶层 Composable 不做数据订阅**——`LaunchedEffect` 收 Flow 只在 `MainActivity` 那种入口点，下游 Composable 接收 **data 对象** 作为参数
- **Composable 参数里不允许出现 Repository 类型**——只接 data class、回调、Modifier
- **格式化函数（`fmtSpd` / `fmtAlt` 等）** 和 **枚举（`SpeedUnit` / `AltUnit` 等）** 未来值得抽出 `ui/format/` 子包；现在还挤在 `GpsScreen.kt` 顶部
- **子组件（`InstrumentCell`、`BaroCell`、`LightMetricRow`）** 目前 `private fun` 挂在 `GpsScreen.kt` 里，数量多起来时拆到 `ui/component/`
- 不要让一个 Composable 返回多个截然不同的 UI（见过 `if (state.loading) X else if (state.error) Y else Z` 这种）——拆成多个 Composable，在调用处分支

### 触及现有坏味道时

遵循"**童子军规则**"：

- **离开比到来时更干净一点**。改一个函数顺手把它的命名、缩进、局部变量换掉
- **不做 "顺便大重构"**：看到 `GpsScreen.kt` 面条不代表可以在 bugfix PR 里顺手把它拆了。**专门开一个 `refactor(ui)` PR**，说明动机、范围、验证方式
- **拆一个坏文件的 PR，不要再顺便加新功能**。保持重构 PR 的 diff 尽量只在移动代码

### 常见反模式（见到就阻止）

这些是维护者看一眼就会"想死"的东西。不要在本仓库出现：

- 千行以上的单文件 UI / Activity
- Repository 里混 UI 状态或格式化字符串
- `when(type)` 每处都要手动加分支的"类型 tag"（考虑 sealed class）
- 静态工具类里堆杂物（`Utils.kt` / `Helper.kt`）——按主题拆专用的 `UnitConverter` / `AttitudeMath`
- 跨层调用（Composable 直接 `context.getSystemService` 开传感器）
- 同一份常量在多个文件散落（必须走 `companion object` 或顶层 `const val`）

### 何时是重构 PR 的好时机

- 准备在某个模块加新功能，发现"得先清理才能干净地加"—— **先开一个 refactor PR，merge 后再开 feature PR**。这是我们用过的节奏：v0.1.2 的 Repository 抽函数 → v0.2.0-alpha.1 的 sensor 扩展
- 子代理 CR 里连续两次指出同一类坏味道
- 文件大小、嵌套深度跨过预警线

---

## 版本号与发布

遵循 [SemVer](https://semver.org/lang/zh-CN/)。预发布用 `-alpha.N` / `-beta.N` / `-rc.N` 后缀。

- `versionCode` 单调递增（整数）
- `versionName` 例：`0.2.0-alpha.1`
- tag 名带 `v` 前缀：`v0.2.0-alpha.1`
- 含 `-` 后缀的 tag 会被 release.yml 识别为 prerelease

**版本号需要同步更新的地方**（遗漏会导致 User-Agent 和包版本不一致）：
- `app/build.gradle.kts`：`versionCode` + `versionName`
- `app/src/main/java/com/gpsplane/app/data/TilePreloader.kt`：`USER_AGENT` 常量
- `app/src/main/java/com/gpsplane/app/ui/screen/MapScreen.kt`：`userAgentValue`
- `app/src/main/java/com/gpsplane/app/ui/screen/DownloadScreen.kt`：`userAgentValue`
- `CHANGELOG.md`

---

## CHANGELOG 规则

遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 规范。**英文撰写**（release.yml 的 awk 抽取脚本依赖 `## [VERSION]` 格式）。

**核心原则：面向用户描述变更，不记 commit 细节（不写哈希、不抄 commit message）。**

变更分类（仅列出有内容的分类）：**Added** / **Changed** / **Deprecated** / **Removed** / **Fixed** / **Security**。

### 日常开发

每个模块级改动（feat / fix / refactor）在 `## [Unreleased]` 段落对应分类下追加一行。小型 chore / docs / style 无需改 CHANGELOG。

```markdown
## [Unreleased]

### Added
- Load factor readout from the raw accelerometer…

### Changed
- Tile cache moved to noBackupFilesDir…
```

### 准备发版（打 tag 前）

1. 将 `## [Unreleased]` 改为 `## [X.Y.Z] — YYYY-MM-DD`
2. 在其上方插入新的空 `## [Unreleased]` 段

**过渡状态**：当前 CHANGELOG 仍保留多版本并存的 `## [X.Y.Z] — Unreleased` 写法（0.1.1 / 0.1.2 / 0.2.0-alpha.1）。未来迁移到上面的标准规范时，选一个时间点把"已合但未 tag"的版本统一整理。

---

## 测试与构建

### 常用命令

```bash
./gradlew test                # 全量单测
./gradlew assembleDebug       # 出 debug APK
./gradlew :app:compileDebugKotlin --rerun-tasks  # 强制重编译
```

### 测试规范

- 新增的纯函数（`data/*.kt` 里的 `object` / `class`）**必须**带单测
- 单测用 JUnit 4 + Truth，位于 `app/src/test/`
- 涉及 `android.location.GnssStatus` 等平台常量的测试，**用硬编码整数值**（因为 stub android.jar 返回 0），并加注释说明为什么
- 不要开 `testOptions.unitTests.isReturnDefaultValues = true`——它会静默把未来测试中意外触达 Android API 的失败转成 0/null 埋 bug

---

## 已知陷阱（踩过的坑）

避免重复踩，按主题记录。

### Flow 组合

- **`combine` 会阻塞到每个上游首发一次**：callback-based 的 sensor repository 必须在 `callbackFlow` 开头 `trySend(EMPTY)`，否则 dashboard 会等慢传感器首 callback 几百 ms。`AttitudeRepository` 和 `EnvironmentRepository` 都已加这个 seed
- **50Hz 传感器直接喂 UI 会闪烁**：`Modifier.weight(1f)` 的 Text 列宽踩到字符边界时会在 1 行/2 行间跳，抖掉整个布局。所有紧凑数值 Text 都加 `maxLines = 1, softWrap = false`
- **高频 sensor 读数需要 EMA 平滑**：50 Hz 原始值的末位数字永远不稳。用 `EmaFilter`（`data/EmaFilter.kt`），α 常用 0.10–0.15
- **Flow 需要 lifecycle-aware**：`MainActivity` 的组合 Flow 用 `flowWithLifecycle(Lifecycle.State.STARTED)`，后台时自动取消订阅、前台回来自动重订

### Android 存储

- `cacheDir` 会被系统清理，不适合存用户主动准备的数据
- `filesDir` 会被 Google Auto Backup 上传（25 MB/app 上限），大文件缓存会截断
- `noBackupFilesDir` 持久且不进 backup——osmdroid 瓦片缓存正确位置

### Android 权限

- 声明但未使用的敏感权限会触发 Play Store 审查。**需要时再加**，v0.1.1 就为此删了 5 个权限
- `ACCESS_BACKGROUND_LOCATION` 必须配合前台服务才生效
- `WRITE_EXTERNAL_STORAGE maxSdkVersion=28` 对内部存储无意义，osmdroid 用 `context.cacheDir` / `filesDir` 不需要它

### 签名与密钥

- `app/stratos.keystore` 在本地开发用；CI 通过 `KEYSTORE_BASE64` secret 提供
- **任何 base64 keystore 副本绝不能进仓库**——`.gitignore` 的 `/local_dev` 是当前安全存放位置
- v0.1.0 早期出现过 `stratos.txt`（keystore 的 base64 拷贝）未被忽略的事故，`.gitignore` 已加补丁

### osmdroid

- `TilePreloader` 和 `MapScreen` 都依赖 `ArcGISTileSource.name()` 作为缓存路径——**改名字会让旧缓存静默失效**
- ArcGIS 瓦片 URL 用 `{z}/{y}/{x}` 不是标准的 `{z}/{x}/{y}`
- MAPNIK（tile.openstreetmap.org）在中国被墙，所以选了 ArcGIS

### 手机硬件差异

- 约 60-70% Android 手机**没有**气压计，UI 必须优雅降级（当前 "no baro" 显示）
- 用 `adb shell dumpsys sensorservice | grep "type: android.sensor"` 可以看某台设备的真实传感器列表
