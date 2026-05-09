# Stratos / GPS-Plane

本文件是给 Codex 的项目级说明。它总结了这个仓库的工作方式、容易踩的坑和必须同步的文件。

## 项目概况

- 项目名：Stratos
- 仓库：`3aKHP/Stratos`
- 应用 ID：`com.gpsplane.app`
- 目标：民航客机窗座场景的 GPS 飞行仪表，包含仪表盘、离线地图、GNSS 卫星图、姿态与气压计读数

## 工作方式

- 先读 `README.md`、`docs/developer/architecture.md` 和相关源码，再动手改。
- 只改当前任务需要的文件，避免顺手重构。
- 没有明确要求时，不要主动 commit、push、merge。
- 需要提交时，使用 Conventional Commits。
- 做 PR review 时，只看最新 head 的代码、checks 和日志，不要依赖旧结论。

## 版本与文档同步

- 每次版本号变更时，同步更新：
  - `app/build.gradle.kts`
  - `app/src/main/java/com/gpsplane/app/data/TilePreloader.kt`
  - `app/src/main/java/com/gpsplane/app/ui/screen/MapScreen.kt`
  - `app/src/main/java/com/gpsplane/app/ui/screen/DownloadScreen.kt`
  - `CHANGELOG.md`
- 用户可见行为变化时，顺手更新 `README.md` 和相关 docs。
- 数据流或架构变化时，优先检查 `docs/developer/architecture.md` 是否过期。

## 代码规范与架构

- 一个文件尽量只做一件事；接近或超过 300 行时，先想能不能拆。
- `ui/screen` 只放 Composable 和布局组织，不直接订阅 `LocationManager` / `SensorManager`。
- Composable 参数里不要传 Repository，只传 data class、回调和 `Modifier`。
- `data/model` 保持纯数据，计算逻辑放到同目录的 `object` / `class`。
- 纯数学、滤波、统计、单位换算应抽成独立 helper，不要散在 Repository 或 UI 里。
- 新逻辑如果出现以下任一情况，优先拆分：
  - 同一公式在多个地方重复
  - 函数或 Composable 超过约 50 行
  - 嵌套层级明显过深
  - 需要单独测试
- 公开 API 有必要时补 KDoc，说明 what / why，不写废话注释。
- 看到历史坏味道时，遵循“离开时比到来时更干净一点”，但不要在 bugfix PR 里顺手做大重构；重构单独开 PR。
- 常见反模式要主动拦住：千行单文件 UI、Repository 混 UI 状态、`Utils.kt` 杂物堆、跨层调用系统服务、同常量散落多处。

## 测试与验证

- 纯函数逻辑放在 `app/src/main/java/com/gpsplane/app/data/` 时，通常要补 `app/src/test/java/...` 单测。
- 完成较大的改动前，至少跑：
  - `./gradlew test`
  - `./gradlew assembleDebug`
- 涉及 UI、权限、传感器、生命周期、存储路径的改动，优先做真机验证。
- JVM 单测不要直接依赖 Android 平台 API；需要时用硬编码常量。

## 已知陷阱

- `combine(...)` 会等所有上游都至少发一次值；`callbackFlow` 需要时先发 `EMPTY`。
- 很多设备没有气压计，UI 必须优雅降级。
- osmdroid 的离线瓦片缓存应放在 `noBackupFilesDir`，不要退回 `filesDir`。
- ArcGIS 瓦片 URL 顺序是 `{z}/{y}/{x}`。
- 高频传感器读数容易闪烁，必要时先做 EMA 平滑，再给 UI。

## 本地笔记

- 临时说明、秘密和一次性草稿放在 `local_dev/`，不要提交进仓库。
