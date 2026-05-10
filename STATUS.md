# STATUS.md — 项目现状

本文件记录 Stratos 的名字/ID 对应、当前版本状态、技术栈、仓库结构与跨 PR 遗留 TODO。需要了解"这个项目是什么、在哪、欠什么"时读这里。

编码、架构、发版细则见 [`docs/developer/style.md`](docs/developer/style.md)。日常工作流见 [`CLAUDE.md`](CLAUDE.md)。

---

## 项目概况

**Stratos** 是一款面向民航客机窗座飞友的 GPS 飞行仪表 Android 应用。把手机变成驾驶舱风格的飞行显示器，展示实时速度/高度/航向/垂直速度/GNSS 卫星信息/姿态/加速度/载荷因子/转弯率/气压计读数，并提供基于 osmdroid + ArcGIS 的离线移动地图。

### 名字与 ID 的对应

| 维度 | 值 |
|---|---|
| Git 仓库 | `3aKHP/Stratos`（早期叫 GPS-Plane，工作目录仍叫 `GPS-Plane`） |
| 展示名 / `app_name` | Stratos |
| `applicationId` | `com.gpsplane.app` |
| 作者 | 3aKHP · 4sljq.top |

### 版本状态

- **v0.1.0** — 已打 tag、已发 Release
- **v0.1.1** — 已合 main，未打 tag（CHANGELOG 仍是 Unreleased）
- **v0.1.2** — 已合 main，未打 tag（CHANGELOG 仍是 Unreleased）
- **v0.2.0-alpha.1** — 已合 main，未打 tag（CHANGELOG 仍是 Unreleased）
- **v0.2.0-alpha.2 / alpha.3** — 已合 main
- **v0.2.0-alpha.4** — 开发中（前台服务 + GPX 记录）

"合了 main 但没 tag" 是刻意的——每个 milestone 打 tag 时一并决策要不要把前面积攒的修订也 tag 出来。

### 技术栈

| 层级 | 技术 |
|---|---|
| UI | Jetpack Compose + Material 3（纯暗色主题） |
| 地图 | osmdroid 6.1.20，ArcGIS World Street Map 瓦片 |
| GPS | `LocationManager`（GPS_PROVIDER，1 Hz） |
| 传感器 | `SensorManager`：rotation vector、linear acceleration、accelerometer、gyroscope、pressure |
| 异步 | Kotlin Coroutines + Flow |
| 构建 | AGP 8.13、Kotlin 2.1、Gradle 8.13，Java 17，compileSdk 36，minSdk 26 |

### 测试设备

- `fcc18eba` — OPPO Find X8 Ultra，**已知无气压传感器**（CABIN 列永远 "no baro"）

---

## 仓库结构速查

```
app/src/main/java/com/gpsplane/app/
├── MainActivity.kt              # 入口、权限、组合 Flow
├── data/
│   ├── GpsRepository.kt         # GPS + GNSS
│   ├── AttitudeRepository.kt    # rotation/linear-accel/accel/gyro
│   ├── EnvironmentRepository.kt # 气压计
│   ├── TilePreloader.kt         # 离线瓦片下载
│   ├── VerticalSpeedFilter.kt   # 纯函数：EMA 垂直速度
│   ├── EmaFilter.kt             # 纯函数：通用 EMA
│   ├── AttitudeMath.kt          # 纯函数：姿态与加速度换算
│   ├── SatelliteStats.kt        # 纯函数：GNSS 统计
│   ├── PressureMath.kt          # 纯函数：ISA 气压↔高度
│   ├── tiles/ArcGISTileSource.kt
│   └── model/{GpsData, AttitudeData, EnvironmentData, SatelliteInfo}.kt
├── ui/screen/{GpsScreen, MapScreen, DownloadScreen}.kt
├── ui/theme/Theme.kt
└── util/UnitConverter.kt

app/src/test/java/com/gpsplane/app/
├── data/*Test.kt                # 纯函数单测，JVM 运行
└── util/UnitConverterTest.kt

docs/
├── user/getting-started.md
└── developer/{architecture, building, testing, style}.md

.github/workflows/
├── ci.yml                       # push/PR 跑 test + assembleDebug
└── release.yml                  # 打 v* tag 触发，签名并发布 Release
```

---

## 遗留 TODO

跨 PR 未完成的事。PR 级别的 TODO 放进各自 PR 描述。

### 版本发布

- **v0.1.1 / v0.1.2 / v0.2.0-alpha.1 是否补 tag**：CHANGELOG 仍是 Unreleased。补 tag 会把 "Unreleased" 字样打进 Release notes

### 架构与功能

- **磁偏角算法选型**（v0.2.0-alpha.2）：已完成（WMM via GeomagneticField）
- **ZULU 时钟 + 飞行时长状态机**（v0.2.0-alpha.2）：已完成
- **前台服务 + GPX 航迹记录**（v0.2.0-alpha.4）：已完成
- **TilePreloader 走廊宽度纬度修正**：当前 `corridorTiles` 忽略 `cos(lat)`，高纬度走廊偏窄。独立 PR
- **Dashboard 无气压计时是否隐藏 BaroRow**：当前保持显示 "no baro"
- **`GpsScreen.kt` 拆分**：已完成（split into ui/format/ + ui/component/）

### 观感 / 性能

- `combine(gps, att, env)` 当前 ~200 Hz 触发 Compose 重组。未来加 `conflate()` 或 `sample(16.ms)` 可省电
- `architecture.md` 未反映 v0.1.2 之后的 Repository 拆分
