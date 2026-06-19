# Architecture

> 上次停留于 v0.1.2。本次刷新到 v0.2.0-beta.2：补 EnvironmentRepository、前台服务、GPX 航迹记录、SkyProjection、MagneticDeclination、FlightTimer、GForceTracker、AstroTime/SunPosition 等模块。

## Data Flow

数据流的**真正 owner** 是前台服务 `GpsTrackingService`，不是 `MainActivity`。这让 GPS / 传感器订阅在屏幕关闭时也能继续，是 v0.2.0-alpha.4 引入的最大架构变化。

```
┌──────────────┐  ┌───────────────────┐  ┌────────────────────┐
│ GpsRepository│  │ AttitudeRepository│  │EnvironmentRepository│
│ (GPS_PROVIDER)│  │(ROTATION_VECTOR) │  │ (TYPE_PRESSURE)    │
│  GnssStatus  │  │ LINEAR_ACCEL      │  │  可选硬件，优雅降级  │
└──────┬───────┘  └────────┬──────────┘  └─────────┬──────────┘
       │ Flow<GpsData>     │ Flow<AttitudeData>    │ Flow<EnvironmentData>
       └─────────┬─────────┴───────────────────────┘
                 │ combine { }  (在 service 的协程里)
        ┌────────┴─────────────────────────────┐
        │        GpsTrackingService            │  ← 前台服务
        │  (持久通知、screen-off 存活)          │
        │                                      │
        │  派生状态：                            │
        │   • FlightTimer   (起降状态机)        │
        │   • MagneticDeclination              │
        │   • SunPositionNoaa / AstroTime      │
        │   • GForceTracker (极限过载)          │
        │   • TrackRecorder (GPX 落盘)          │
        └────────┬─────────────────────────────┘
                 │ StateFlow ×N (via LocalBinder)
        ┌────────┴────────┐
        │   MainActivity  │  ← 通过 rememberBoundService() 订阅
        └────────┬────────┘
                 │ Compose State
    ┌────────────┼────────────┐
    │            │            │
┌───┴───┐  ┌────┴────┐  ┌───┴────────┐
│GpsScr.│  │MapScr.  │  │DownloadScr.│
└───────┘  └─────────┘  └────────────┘
```

## Component Responsibilities

### GpsTrackingService（数据流核心）

前台服务，进程内单例。v0.2.0-alpha.4 引入，是 v0.2.0 最大的架构改动。

- **持有** `GpsRepository` / `AttitudeRepository` / `EnvironmentRepository` / `TrackRecorder`，在 `onCreate` 一次性构造
- **订阅** `combine(gps, att, env)`，在 service 自己的 `CoroutineScope(SupervisorJob + Dispatchers.Default)` 里收集
- **派生** 一系列 `StateFlow` 暴露给 UI：`gps` / `attitude` / `environment` / `flight` / `declinationDeg` / `gForce` / `sunTimes` / `recording` / `recordingEnabled`
- **持久通知** 显示飞行阶段（GROUND / T+HH:MM:SS），带 Stop action；screen-off 下保活
- **生命周期**：UI unbind 不停服务；只有 `stop()`（通知 action 或系统回收）才真正拆解
- **binder**：`LocalBinder` 暴露 service 实例，`MainActivity` 用 `rememberBoundService()` 订阅

`TrackRecorder.enabled` 的切换和 GPS 采样在**同一协程**里处理，避免文件 Writer 在写盘中途被关闭。

### GpsRepository

- `LocationManager.requestLocationUpdates(GPS_PROVIDER, 1s, 0m)`
- `GnssStatus.Callback` 拿每颗卫星的 SNR / azimuth / elevation
- GPS 高度差分 + EMA 低通滤波得垂直速度
- 会话级 TTFF 跟踪（`SystemClock.elapsedRealtime()`）
- 生命周期：回调在 `callbackFlow` 注册、`awaitClose` 清理；首次发 `EMPTY` 不阻塞 `combine`

### AttitudeRepository

- `SensorManager`：`TYPE_ROTATION_VECTOR`（azimuth/pitch/roll）、`TYPE_LINEAR_ACCELERATION`、`TYPE_ACCELEROMETER`、`TYPE_GYROSCOPE`
- `SENSOR_DELAY_GAME`（~20ms）更新率
- 每个传感器事件发一个 `AttitudeData`

### EnvironmentRepository

- `TYPE_PRESSURE` 气压计；很多设备没有，返回 `EnvironmentData.EMPTY` 优雅降级
- 与 AttitudeRepository 分开，因为读数节奏和"缺传感器"语义不同（IMU 必有，气压计可选）
- `callbackFlow` 先发 `EMPTY` 种子，避免阻塞 `combine`

### TilePreloader

- 大圆航线采样 → 瓦片坐标枚举 → 走廊扩展 → 并发下载（8 路，`Semaphore` 控制）写入 osmdroid 文件缓存
- 缓存格式：`{tileSourceName}/{z}/{x}/{y}.tile` —— `MapTileFilesystemProvider` 直接读
- 纯几何 helper（`greatCircleDistance` / `interpolate` / `bresenhamLine`）抽成 companion object 静态方法，可单测

### TrackRecorder

- 一份 GPX 文件的生命周期所有者：起飞（phase → AIRBORNE）开文件，落地（→ GROUND）关文件
- 文件名 `tracks/YYYYMMDD-HHMMSS.gpx`（UTC），落在 `filesDir/tracks`（Auto-Backup 友好，单次航班 < 1 MB）
- **非线程安全**：所有访问（`onGpsSample` / `enabled` setter / `close`）必须串行化，由 service 的采集协程保证
- 周期 flush（默认每 10 点），崩溃只丢尾巴
- `enabled = false` 可中途关录制而不打扰其它服务职责

### MainActivity

- **不直接订阅** `LocationManager` / `SensorManager`（AGENTS.md 硬规则）
- 通过 `rememberBoundService()` 绑定 service，`collectAsState()` 各 `StateFlow`
- 权限：位置权限 + POST_NOTIFICATIONS（API 33+）一次性请求；权限拿到后 `GpsTrackingService.start()`
- 沉浸模式：`WindowInsetsControllerCompat`，`onWindowFocusChanged` 重新打 flag（部分 OEM 皮肤会重置）

### MapScreen

- osmdroid `MapView` 经 `AndroidView` 嵌入
- ArcGIS tile source，URL 顺序 `{z}/{y}/{x}`（覆盖 `OnlineTileSourceBase.getTileURLString`）
- `MyLocationNewOverlay` 定位指示
- 用户可 pan/zoom；首 fix 只自动居中一次

### GpsScreen（已拆分）

- 顶层 Composable 只做布局组织，订阅 service 的 StateFlow
- 子组件拆到 `ui/component/` 和 `ui/format/`：
  - `SkyPlot` — Canvas 绘制的星空图（航向稳定、TRK-UP / HDG-UP 切换、磁北标记）
  - `SignalBars` — 按星座分组的 SNR 信号条
  - `InstrumentRow` / `TopBar` / `BottomInfo` — 仪表格
  - `BaroRow` — 气压计行（无气压计时显示 "no baro"）
  - `UnitConfigSheet` / `GForcePopup` — 底部 sheet / 弹窗
  - `Formatters` / `Units` — 格式化与单位

## Pure Helpers（可单测）

v0.2.0 把计算逻辑从 Repository / UI 里抽成纯函数，全部在 `app/src/test/` 有 JVM 单测：

| Helper | 职责 |
|---|---|
| `VerticalSpeedFilter` | GPS 高度差分 + EMA（α=0.3）得垂直速度 |
| `EmaFilter` | 通用 EMA |
| `AttitudeMath` | 姿态与加速度换算（`magnitudeInG` 等） |
| `SatelliteStats` | GNSS 统计 |
| `PressureMath` | ISA 气压 ↔ 高度 |
| `MagneticDeclination` | 经纬度 + 时间 → 磁偏角（WMM via `GeomagneticField`） |
| `FlightTimer` | 起降状态机纯函数（GROUND / AIRBORNE） |
| `GForceTracker` | 极限过载 min/max 记录 |
| `SkyProjection` | 卫星 azimuth/elevation → 极坐标投影 |
| `AstroTime` | Julian Day / 均时差 / 视太阳时（Meeus 算法，±0.07s） |
| `SunPositionNoaa` | NOAA 日出日落，用本地视太阳日期避免东经 24h 偏差 |

## Key Design Decisions

**前台服务拥有数据流（v0.2.0-alpha.4）**：v0.1 时 MainActivity 直接 `combine(gps, att)`，屏幕一关 GPS 就停——这是窗座场景最大的体验缺口。v0.2.0 把订阅移进前台服务，UI 退化为纯订阅者。副作用：未来要做航迹回放、分享，都能复用同一份 service 状态。

**GPS_PROVIDER over FusedLocationProvider**：高空 WiFi/cell 三角不可用。裸 GPS 让我们直接控制采样并拿到 `GnssStatus` 卫星元数据。

**GPS 高度差分做垂直速度（不用气压计）**：客舱增压到 ~8000 ft 等效，气压计垂直速度在巡航段会读近零。GPS 高度差分 + EMA 有噪声但方向正确。

**Widget 化、非滚动仪表盘**：舱内使用要求一瞥可读，纵向每一像素都要预算——星空图用 `Modifier.weight(1f)` 撑满。

**ArcGIS tile source**：MAPNIK（tile.openstreetmap.org）在国内被墙。ArcGIS Online World Street Map 可达，航线可视化够用。

**飞行中无 tile server**：瓦片必须起飞前预下载。preloader 直接写 osmdroid 的文件缓存格式，map provider 链是 cache 先于 network。

**纯函数 helper 抽离**：v0.1.2 起持续做，让 Repository/UI 只剩编排，数学/滤波/统计单独可测。这是 `data/` 目录增长的纪律。

## 已知性能 / 待办

- `combine(gps, att, env)` 当前 ~200 Hz 触发 Compose 重组；未来可加 `conflate()` 或 `sample(16.ms)` 省电
- `TilePreloader.expandCorridor` 走廊宽度忽略 `cos(lat)`，高纬偏窄（v0.2.0 收尾计划修）
- POST_NOTIFICATIONS 被拒时无 in-app 提示（CR 遗留 S3，v0.2.0 收尾计划修）
