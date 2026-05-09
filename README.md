# Stratos

**民航客机 GPS 飞行仪表 / GPS Flight Instrument for Commercial Aviation**

[English](#english) below

---

## 简介

Stratos 把你的手机变成驾驶舱风格的飞行显示器。为靠窗座位的飞友打造：实时速度、高度、航向、垂直速度、卫星星座、离线地图上的实时位置——无需网络，一屏尽览。

## 功能

- **航空仪表盘** — 速度、高度、垂直速度、航向/航迹、马赫数、俯仰/滚转、加速度。双单位并排显示（kn/kmh、ft/m 等），每项可独立配置。
- **离线地图 + 航线预加载** — 基于 ArcGIS 的移动地图。起飞前沿大圆航线走廊预下载瓦片，飞行中完全离线可用。
- **卫星星空图** — 实时极坐标图展示可见 GNSS 卫星（GPS、GLONASS、北斗、Galileo、QZSS），含 SNR 信号条、方位角/仰角、星座配色。
- **指南针与姿态** — 星空图上叠加航迹红箭头 + 手机朝向扇形指示器。人工地平线展示俯仰/滚转。
- **GNSS 星座详情** — 每种星座的卫星数量和信号质量独立展示。
- **单位自定义** — 每项测量选两个单位，同时显示。默认预选航空标准单位。

## 截图

> *v0.2.0 中补充*

## 技术栈

| 层级 | 技术 |
|---|---|
| UI | Jetpack Compose + Material 3 |
| 地图 | osmdroid 6.1.20（ArcGIS 瓦片源） |
| GPS | `LocationManager`（GPS_PROVIDER，1 Hz） |
| 传感器 | `SensorManager`（ROTATION_VECTOR、LINEAR_ACCELERATION） |
| 异步 | Kotlin Coroutines + Flow |
| 构建 | AGP 8.13、Kotlin 2.1、Gradle 8.11 |

## 系统要求

- Android 8.0+（API 26）
- 设备需具备 GPS/GNSS 硬件
- 完整体验需：电子罗盘、加速度计、陀螺仪

## 快速开始

```bash
git clone <repo-url>
cd GPS-Plane
# 用 Android Studio 打开，或：
./gradlew assembleDebug
```

签名配置未包含在源码中。将你的 keystore 放在 `app/stratos.keystore` 并在 `app/build.gradle.kts` 中配置 `signingConfig`。

## 项目结构

```
app/src/main/java/com/gpsplane/app/
├── MainActivity.kt              # 入口：标签页导航、权限控制
├── data/
│   ├── GpsRepository.kt         # GPS 数据（LocationManager + GnssStatus）
│   ├── AttitudeRepository.kt    # IMU 数据（SensorManager）
│   ├── TilePreloader.kt         # 离线瓦片下载与缓存
│   └── model/
│       ├── GpsData.kt           # GPS 快照数据类
│       ├── AttitudeData.kt      # IMU 快照数据类
│       └── SatelliteInfo.kt     # 单颗卫星元数据
├── ui/
│   ├── screen/
│   │   ├── GpsScreen.kt         # 仪表盘（飞行仪表、星空图、信号条）
│   │   ├── MapScreen.kt         # 离线地图 + 定位叠加
│   │   └── DownloadScreen.kt    # 瓦片预加载界面
│   └── theme/
│       └── Theme.kt             # Material 3 暗色主题
└── util/
    └── UnitConverter.kt         # 单位换算 + 马赫数
```

## 文档

| 文档 | 面向 |
|---|---|
| [快速入门](docs/user/getting-started.md) | 用户 |
| [架构设计](docs/developer/architecture.md) | 开发者 |
| [构建指南](docs/developer/building.md) | 开发者 |
| [测试指南](docs/developer/testing.md) | 开发者 |

## 作者

**[3aKHP](https://github.com/3aKHP)** · <https://4sljq.top>

## 许可证

[Apache License 2.0](LICENSE) © 2026 3aKHP

---

## English

## Overview

Stratos turns your phone into a cockpit-style flight display. Built for window-seat aviation enthusiasts who want real-time speed, altitude, heading, vertical velocity, satellite constellations, and position on an offline map — all in one screen, without network.

## Features

- **Avionics dashboard** — Speed, altitude, vertical speed, heading/track, Mach number, pitch/roll, acceleration. Dual-unit display (kn/kmh, ft/m, etc.), configurable per metric.
- **Offline map with route preloading** — ArcGIS-based moving map. Pre-download tiles along a great-circle route corridor before takeoff; works entirely offline in flight.
- **Satellite sky plot** — Real-time polar chart of visible GNSS satellites (GPS, GLONASS, BeiDou, Galileo, QZSS) with SNR bars, azimuth/elevation, and constellation colors.
- **Compass & attitude** — Bearing needle (GPS track) and phone-compass fan indicator overlaid on the sky plot. Artificial horizon for pitch/roll.
- **GNSS constellation breakdown** — Per-constellation satellite counts and signal quality.
- **Unit customization** — Pick two units for each measurement; display both simultaneously. Aviation defaults pre-selected.

## Tech Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Maps | osmdroid 6.1.20 (ArcGIS tile source) |
| GPS | `LocationManager` (GPS_PROVIDER, 1 Hz) |
| Sensors | `SensorManager` (ROTATION_VECTOR, LINEAR_ACCELERATION) |
| Async | Kotlin Coroutines + Flow |
| Build | AGP 8.13, Kotlin 2.1, Gradle 8.11 |

## Requirements

- Android 8.0+ (API 26)
- Device with GPS/GNSS hardware
- For full experience: compass, accelerometer, gyroscope

## Docs

| Doc | Audience |
|---|---|
| [Getting Started](docs/user/getting-started.md) | Users |
| [Architecture](docs/developer/architecture.md) | Developers |
| [Building](docs/developer/building.md) | Developers |
| [Testing](docs/developer/testing.md) | Developers |

## Author

**[3aKHP](https://github.com/3aKHP)** · <https://4sljq.top>

## License

[Apache License 2.0](LICENSE) © 2026 3aKHP
