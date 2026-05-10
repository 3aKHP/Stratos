# Roadmap / 路线图

> English version below. 中文版见下方。

---

## v0.2.0 — Cockpit & Sensors / 驾驶舱与传感器

### Barometer / 气压计
- [x] **Cabin Altitude / 座舱高度** — 手机气压计换算高度，与 GPS 真实高度对比，展示客舱压差 *(alpha.1)*
- [x] **Cabin Pressure / 座舱气压** — 直接显示 hPa 或 inHg *(alpha.1)*

### Time & Aviation Standards / 时间与航空规范
- [x] **ZULU Time / 协调世界时** — UTC 时间带 `Z` 后缀，本地时间旁边显示 *(alpha.2)*
- [x] **Flight Time / 飞行时长** — 检测地速 >150kn 且高度 >3000ft 自动计时，落地停止 *(alpha.2)*
- [x] **Magnetic Declination / 磁偏角** — 根据 GPS 坐标计算真北与磁北偏差 *(alpha.2)*

### Dynamics & Turbulence / 动态与颠簸
- [ ] **G-Meter Min/Max / 极限过载** — 记录本次飞行最大正 G 和最小负 G *(beta.1)*
- [x] **Turn Rate / 转弯率** — 陀螺仪偏航角速度（°/sec） *(alpha.1)*

### Environment / 环境
- [ ] **Sunrise/Sunset / 日出日落** — 基于 GPS 坐标的天文算法，无网计算当前位置日出日落时间 *(beta.1)*

### Flight Tracking / 飞行记录
- [x] Foreground service with persistent notification（前台服务防 GPS 休眠） *(alpha.4)*
- [x] Flight track recording / 航迹记录（GPX 导出） *(alpha.4)*
- [x] Takeoff/landing auto-detection / 起降自动检测 *(alpha.2 state machine + alpha.4 drive)*
- [ ] Full-screen / immersive mode / 全屏沉浸模式 *(beta.1)*

### Polish / 打磨
- [ ] Instrumentation tests / 仪器测试
- [x] Release signing configuration / 发布签名配置 *(v0.1.0)*

### Outstanding before v0.2.0 / v0.2.0 发版前待办
- [ ] TilePreloader 走廊宽度 `cos(lat)` 修正（高纬度偏窄）— 等航班实测
- [ ] `architecture.md` 刷新（v0.1.2 之后累积的 Repository 拆分、SkyProjection、MagneticDeclination、FlightTimer、GpsScreen 拆分、前台服务层）
- [ ] POST_NOTIFICATIONS 被拒时的 in-app banner（CR 遗留 S3）
- [ ] `[Unreleased]` / "已合未 tag" 版本（0.1.1 / 0.1.2 / alpha.1）的补 tag 决策

---

## v0.3.0 — Navigator / 导航

- [ ] **Waypoint input / 航点输入** — 城市名/机场代码（地理编码）
- [ ] **Distance to Destination / 距目的地** — 直线距离实时计算
- [ ] **Bearing to Destination / 目标方位角** — 导航箭头指向目的地
- [ ] **ETA / 预计到达** — 基于当前地速和剩余距离推算
- [ ] Multiple tile source options / 多瓦片源（卫星图、地形图）
- [ ] Configurable corridor width presets / 可配置走廊宽度预设

---

## v1.0.0 — Public Release / 公开发布

- [ ] Play Store listing / 应用商店上架
- [ ] Privacy policy / 隐私政策
- [ ] Full ZH + EN localization / 完整中英文本地化（所有字符串入资源文件）
- [ ] Screenshots and promotional assets / 截图与宣传素材
- [ ] Crash reporting / 崩溃上报（Firebase Crashlytics）
- [ ] Open-source license / 开源许可证选定

---

## English

### v0.2.0 — Cockpit & Sensors

**Barometer**: cabin altitude (phone barometer vs GPS true altitude), cabin pressure (hPa / inHg). *Shipped in alpha.1.*
**Time & Aviation**: ZULU/UTC clock with `Z` suffix, automatic flight-time timer (triggered by speed >150kn at >3000ft), magnetic declination from GPS position. *Shipped in alpha.2.*
**Dynamics**: turn-rate indicator via gyroscope (°/sec) shipped in alpha.1; G-meter recording max/min g-force during flight is scheduled for beta.1.
**Environment**: sunrise/sunset computed locally via astronomical algorithm from GPS position and time. *Scheduled for beta.1.*
**Flight Tracking**: foreground service for persistent GPS, GPX track recording, and auto takeoff/landing detection shipped in alpha.4; full-screen immersive mode is scheduled for beta.1.

### v0.3.0 — Navigator

Waypoint input (city/airport code geocoding), distance and bearing to destination, ETA from ground speed, multiple tile source options, configurable corridor presets.

### v1.0.0 — Public Release

Play Store listing, privacy policy, full i18n, screenshots, crash reporting, open-source license.
