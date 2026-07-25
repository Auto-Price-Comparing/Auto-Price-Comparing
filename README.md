# 外卖比价助手（Auto-Price-Comparing）

双平台（美团 / 淘宝闪购）本地比价 Android App。通过无障碍服务读取前台外卖 App 界面，悬浮窗显示跨平台实付价对比与最优策略。全部本地运行，无服务器。

> **当前状态（重要）**：本仓库目前是 C 的 **fixtures 驱动外壳**——UI、数据管线、引擎、生命周期、采集编排接口均已就位，但**采集核心（读真实外卖 App 价格）尚未实现**：`CollectorAccessibilityService` 是空 stub，美团/淘宝闪购的 `ParserInterface` 实现都不存在，悬浮窗与各屏**只显示内置假数据**。真实比价能力是 A/B 的活（见「成员边界」）。本工程**从未编译/运行验证**（开发机无 JDK/Android SDK），首次 sync 可能有零星编译错，把第一条贴出来我修。

## 技术栈

- Kotlin + 原生 Android（`minSdk 26`、`targetSdk 34`、`compileSdk 34`）
- Room（SQLite）历史库，v2 + 破坏式迁移
- `AccessibilityService` 采集 + `WindowManager` 悬浮窗（`specialUse` 前台服务）
- Kotlinx Coroutines / Flow
- JUnit 单测（6 个测试类）

## 工程结构

```
app/src/main/kotlin/com/team/pricecompare/
├─ App.kt                      Application：进程级注册无障碍开关观察
├─ data/
│  ├─ Models.kt               契约：StoreInfo / ItemPrice / UserDealInput / Deal
│  ├─ db/                     AppDatabase + 4 实体 + 4 DAO（snapshots/items/deals/matches）
│  └─ repo/
│     ├─ FixtureProvider       内置假数据（M0 兜底）
│     ├─ StoreRepository       Flow<List<StoreInfo>> + push/persist/recordAll/historyFor/seedIfEmpty
│     ├─ MatchMemory           confirmed: StateFlow<Set<NamePair>> + confirm()
│     └─ FakeCollectionOrchestrator  M4 fixtures 假编排（延时模拟采集）
├─ engine/
│  ├─ ActualPriceCalculator    实付价：商品+包装+配送−满减−红包−券（归零）
│  ├─ SnapshotPricer           历史参考价（纯函数）
│  ├─ store/StoreNameNormalizer 店名归一（NFKD 全角→半、去括号内容）
│  ├─ match/ProductMatcher     Levenshtein 匹配：自动≥0.85 / 待确认0.6–0.85 / 未配
│  └─ strategy/StrategyRecommender  最优平台 + reason + perPlatform
├─ overlay/
│  ├─ OverlayController        无障碍开关 StateFlow + bind/refresh/ensureService
│  ├─ OverlayService          前台服务 + WindowManager 悬浮窗
│  └─ OverlayView             对比卡 + 红包输入 + 确认按钮 + 未开服务兜底
├─ parsers/                     ← A/B 的领地（C 只定义接口）
│  ├─ ParserInterface          fun parse(root): StoreInfo?
│  ├─ CollectionOrchestrator  接口 + CollectionState（InProgress/Completed/Failed）
│  ├─ SafeParse               try/catch 容错包装（A 调用即不崩）
│  └─ CollectorAccessibilityService  ⚠ 空占位，待 A 实现 dump + 节点遍历
└─ ui/
   ├─ MainActivity            入口 + 4 按钮（启动悬浮窗/商家分析/一键全采/关闭电池优化）+ 三屏切换
   ├─ MerchantAnalysisView    跨平台对比表 + 历史图 + 记录快照
   ├─ ChartView               Canvas 折线 + 填充
   └─ CollectionView          V2 全采屏（店名输入 + 进度 + 结果）
```

单测在 `app/src/test/kotlin/...`：`ActualPriceCalculatorTest`、`SnapshotPricerTest`、`StoreNameNormalizerTest`、`ProductMatcherTest`、`StrategyRecommenderTest`、`OverlayControllerMatchTest`、`FakeCollectionOrchestratorTest`。

## 数据流（fixtures 驱动）

```
FixtureProvider ──→ StoreRepository (_stores: Flow)
                       │  push() 即协程落库 + 5s 去重 ─→ Room 快照
                       │  historyFor() ──→ ChartView 折线
                       ↓
OverlayService.collect ──→ OverlayView（对比卡 + 红包重算 + 匹配概要）
                       ↑ setServiceEnabled ← OverlayController.accessibilityEnabled
                       ↑ setConfirmed      ← MatchMemory.confirmed ←─ confirm() 闭环

CollectionView ──→ FakeCollectionOrchestrator.collect() ──→ Completed(stores) ──→ recordAll
```

## 成员边界

详见 [`AGENTS.md`](./AGENTS.md)。**全组只有一个 app 外壳（本仓库）**，A/B 不另建 app，只填实现。

| 成员 | 拥有 | 禁止触碰 |
|---|---|---|
| A | `parsers/CollectorAccessibilityService`、美团 `ParserInterface` 实现、真 `CollectionOrchestrator`（手势） | `engine/`、`overlay/`、`ui/`、`data/`、Manifest、build 配置 |
| B | `parsers/` 下 flash 解析器、`AppLauncher.kt` | 同上 |
| C | `data/`、`engine/`、`overlay/`、`ui/`、构建配置 | `parsers/` 节点树代码 |

对接面（C 定义，A/B 实现）：`data/Models.kt`、`parsers/ParserInterface.kt`、`parsers/CollectionOrchestrator`、`data/repo/FixtureProvider` 的 JSON 化（待做：A/B dump 真节点树进 `assets/fixtures/`）。

## 构建

> 仓库暂未提交 Gradle wrapper jar。首次用：本机装 Gradle 8.x 跑 `gradle wrapper` 生成 `gradlew`+jar 并补提交；或用 Android Studio 打开根目录让 IDE 处理。

```bash
./gradlew test               # 单测（应全绿）
./gradlew assembleDebug       # 构建 debug APK
./gradlew installDebug        # 装机到已连接真机
./gradlew lint                # Android Lint
```

真机需：开发者选项 + USB 调试；授予悬浮窗权限；开启「外卖比价助手」无障碍服务；关闭电池优化（App 内按钮）。

## 保活与 ROM 适配

国产 ROM 杀后台是最大工程坑，装机后必做：

- 小米/红米：设置 → 应用 → 外卖比价助手 → 省电策略「无限制」+ 自启动开
- 华为/荣耀：设置 → 电池 → 启动管理 → 改为「手动管理」并全开
- OPPO/vivo：设置 → 电池 → 应用耗电管理 → 允许后台运行
- 通用：App 内「关闭电池优化」按钮，或 `设置 → 应用 → 特殊访问 → 忽略电池优化`
- 无障碍服务被关后需重新手动开启（系统限制）

## 构建 Release APK（侧载）

```bash
./gradlew assembleRelease          # 产物：app/build/outputs/apk/release/app-release.apk
```

release 用 debug 签名（`signingConfig = debug`），便于组内自测侧载；正式分发需自备 keystore 并替换 `signingConfigs`。`adb install -r app-release.apk` 装机。

## 当前进度

- M0（完成）：C 脚手架
- M1 早期（完成，fixtures 驱动）：引擎单测、`StoreRepository`、悬浮窗 v2 对比卡、`OverlayController` 生命周期、采集即落库
- M2（完成）：店名/商品匹配 + 策略 + 手动确认闭环
- M3（完成）：商家分析屏 + 历史价格曲线（demo 店播种）
- M4（完成，fixtures 假编排）：一键全采屏 + 进度 + 结果
- M5（完成）：容错包装、保活、lint、release 侧载签名

待 A/B：真 `CollectorAccessibilityService` 节点遍历、美团/flash `ParserInterface` 实现、真 `CollectionOrchestrator` 手势、`fixtures/` 真节点树 dump。

详见 `AGENTS.md`「当前进度」段。

## 合规提示

仅限个人学习自用，侧载 APK，不传播；V2 自动采集绝不碰下单/支付流程。
