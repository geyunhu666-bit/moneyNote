# 记账本 (MoneyNote)

一个简洁、无广告、完全离线的安卓记账应用。使用 Kotlin + Jetpack Compose 构建，数据只保存在本地，不联网、无账号、无后端，数据不出手机。

[English](README.en.md)

## 功能特性

- **记一笔**：支持支出、收入、转账三种类型；自定义金额键盘；一级分类宫格 + 二级分类；转账支持转出/转入账户与手续费；可填写备注、日期、时间，支持编辑与删除。
- **统计报表**：按分类展示支出/收入构成（环形图）与排行，可切换月份与收支类型。
- **资产账户**：支持现金、储蓄卡、信用卡、电子钱包、投资等多种账户；净资产实时汇总；可增删改账户、设置期初余额与图标。
- **日历视图**：月历网格（周一起始），每天显示支出摘要，选中日期可查看当日流水。
- **搜索**：支持关键词（备注/分类/账户）、类型、分类、账户、金额区间、时间范围（不限/本月/上月/近三月）多条件组合筛选。
- **预算**：月度总预算 + 分类预算，显示已用/剩余或超支进度，统计口径与报表一致（含二级分类）。
- **分类管理**：支出/收入分类分栏，支持二级分类、自定义图标、增删改；预置分类不可删除。
- **数据与备份**：导出完整 JSON 备份并可从备份恢复；导出 CSV（带 UTF-8 BOM，Excel/WPS 可直接打开不乱码）。
- **外观**：深色模式 + Material You 动态取色（Android 12+），可跟随系统或手动切换。
- **隐私**：无网络权限、无账号体系、无广告，所有数据仅存于本机。

## 技术栈

- **语言与 UI**：Kotlin、Jetpack Compose（Material 3）
- **架构组件**：ViewModel、Lifecycle、Flow/Coroutines
- **持久化**：Room（KSP）、DataStore Preferences
- **备份编解码**：kotlinx.serialization（JSON）
- **构建**：Gradle Kotlin DSL、版本目录（Version Catalog）

| 组件 | 版本 |
|---|---|
| AGP / Gradle | 9.0.1 / 9.1.0 |
| Kotlin | 2.3.20 |
| Compose BOM | 2026.03.01 |
| Room / KSP | 2.8.5 / 2.3.12 |
| DataStore | 1.1.7 |
| kotlinx-serialization-json | 1.11.0 |
| compileSdk / targetSdk | 36 |
| minSdk | 26（Android 8.0） |
| Java / Kotlin toolchain | 17 |

## 快速开始

### 环境要求

- JDK 17
- Android SDK（`compileSdk 36`）
- 建议使用最新版 Android Studio

### 构建 APK

```bash
./gradlew assembleDebug
# Windows: gradlew.bat assembleDebug
```

构建产物位于 `app/build/outputs/apk/debug/app-debug.apk`，可直接安装到 Android 8.0 及以上的设备。

### 运行单元测试

```bash
./gradlew testDebugUnitTest
```

## 架构与设计

- **单一写入口**：所有写操作统一经过 `BookkeepingRepository`，视图层不直接接触 DAO。
- **金额以“分”存储**：金额全程使用 `Long`（单位：分），仅在界面边界用 `BigDecimal` 做元换算，避免浮点误差。
- **日期以整数存储**：`epochDay`（自 1970-01-01 起的天数）+ `minuteOfDay`，按月/日分组为纯整数运算，不涉及时区字符串。
- **余额为推导值**：账户余额 = 期初余额 + 全部流水影响，账目是唯一事实来源，不落冗余余额字段。
- **备份格式独立于数据库结构**：通过 DTO 层解耦 Room 实体与文件格式，`ignoreUnknownKeys` 保证前后兼容；恢复备份在单事务中整体替换，避免残缺数据。
- **纯函数 + 单元测试**：可脱离 Android 的逻辑（金额换算、月快照、日历网格、预算进度、搜索过滤、CSV 导出、备份编解码）均抽为纯函数并配测试。
- **状态驱动导航**：一级 Tab + 日历/搜索覆盖层 + 编辑器，固定层次，无需导航库。

## 目录结构

```
app/src/main/java/com/witsky/moneynote/
├── data/
│   ├── BookkeepingRepository.kt   # 唯一写入口 / 业务仓储
│   ├── MonthSnapshot.kt           # 月快照纯函数
│   ├── CalendarGrid.kt            # 日历网格纯函数
│   ├── BudgetProgress.kt          # 预算进度纯函数
│   ├── SearchFilters.kt           # 搜索过滤纯函数
│   ├── CsvExport.kt               # CSV 导出纯函数
│   ├── BackupCodec.kt             # JSON 备份编解码
│   ├── BackupPayload.kt           # 备份 DTO 与实体映射
│   ├── local/                     # Room 数据库、DAO、实体、预置数据
│   └── settings/                  # DataStore 设置（主题 / 引导）
└── ui/
    ├── MoneyNoteApp.kt            # 主导航壳
    ├── bills/    # 流水首页
    ├── stats/    # 统计报表
    ├── assets/   # 资产账户
    ├── mine/     # 我的 / 设置（含分类管理）
    ├── edit/     # 记账编辑器
    ├── calendar/ # 日历视图
    ├── search/   # 搜索
    ├── budget/   # 预算管理
    ├── data/     # 数据与备份
    ├── onboarding/ # 首次启动引导
    ├── common/   # 通用组件（金额、日期、进度条等）
    └── theme/    # 主题（颜色、字体、Material 3）
```

## License

待定（可在发布前自行补充）。
