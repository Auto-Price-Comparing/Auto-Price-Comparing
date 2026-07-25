# AGENTS.md — 外卖比价助手

双平台（美团 meituan / 淘宝闪购 flash）本地比价 App。Android 原生 + Kotlin，`minSdk 26`，真机开发。

## 构建

本仓库不含 Gradle wrapper jar（生成环境无 Gradle）。用 Android Studio 打开 `takeaway-compare/`，让 IDE 生成 wrapper 后 sync，或在本机装 Gradle 8.x 后于根目录执行 `gradle wrapper` 生成 `gradlew`。

常用命令（生成 wrapper 后）：

```bash
./gradlew assembleDebug            # 构建 debug APK
./gradlew installDebug             # 装机到已连接真机
./gradlew lint                     # Android Lint
./gradlew :app:dependencies        # 依赖树
```

无单元测试框架引入（M0 阶段）；M1 起补 `app/src/test/` 与 `app/src/androidTest/`。

## Git / 提交与推送

- **推送前必须获得用户明确认可**：任何 `git push`（含 `git push --force` / `--force-with-lease`、推 `main` 等共享分支）执行前都要先向用户说明将推送的内容（改动文件 / commit message / 目标分支）并等待确认，未获认可不得推送。
- 本地 `git add` / `git commit` 可自行执行；`git push` 是与远端同步的边界动作，需把关。
- 远端：`git@github.com:Auto-Price-Comparing/Auto-Price-Comparing.git`，默认分支 `main`。SSH 密钥已配置（GitHub 用户 `Joooa-code`）。
- 分支策略：A/B 建议开功能分支 PR 合 `main`；C 短分支或直接 `main`，但推送前同样需认可。

## 模块与成员边界

| 成员 | 拥有目录 | 禁止触碰 |
|------|----------|----------|
| A · 采集框架 + 美团 | `parsers/CollectorAccessibilityService.kt`、`parsers/ParserInterface.kt` 的美团实现、V2 手势框架 | `engine/`、`overlay/`、`ui/`、`data/` |
| B · 淘宝闪购 | `parsers/` 下的 flash 解析器、`AppLauncher.kt` | 同上 |
| C · 逻辑+UI+工程 | `data/`、`engine/`、`overlay/`、`ui/`、构建配置 | `parsers/` 内任何节点树代码 |

唯一对接面：
- `data/Models.kt` — `StoreInfo` / `ItemPrice` / `UserDealInput` / `Deal`（改字段须三人协商，并同步 fixtures）
- `parsers/ParserInterface.kt` — C 定义接口，A/B 各自实现 `fun parse(root): StoreInfo?`

## fixtures 工作流

A/B 把真实外卖 App 节点树 dump 成 JSON 放入 `app/src/main/assets/fixtures/`（目录待建），C 的 `data/repo/FixtureProvider.kt` 当前用内置假数据，M1 起改为读 JSON。

## 当前进度

- **M0（完成）**：C 脚手架——工程结构、`Models.kt`、Room 空壳、`FixtureProvider`、`ActualPriceCalculator`、`OverlayService`+`OverlayView`、`MainActivity`、Manifest 与资源。出口标准：真机打开美团店铺页，悬浮窗稳定浮于上方并显示假数据。
- **M1 早期（完成，fixtures 驱动）**：`ActualPriceCalculator` JUnit 单测（`app/src/test/`）；`StoreRepository`（`Flow<List<StoreInfo>>` + Room 持久化 + `StoreInfoEmitter` 供 A/B push；`push()` 即协程落库 + 5s 去重；`StoreSnapshotDao.observeByName` 备 M3 曲线）；`OverlayView` v2 双平台对比卡（美团/淘宝闪购实付价并排、最优高亮、可收起、红包 `EditText` 喂 `UserDealInput` 实时重算、无障碍未开时显示「请先开启无障碍服务」兜底）；`OverlayService` 订阅 Repository Flow + 控制器 `accessibilityEnabled` + 编辑模式窗口标志切换。
- **P4（完成）**：`overlay/OverlayController.kt`——绑定无障碍开关状态。`ContentObserver` 监听 `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`，对外暴露 `accessibilityEnabled: StateFlow<Boolean>`；`ensureService(context)` 在开启 + 悬浮窗权限已授时 `startForegroundService(OverlayService)`、否则 `stopService`，由 `collect` 联动 + `MainActivity.onResume` 兜底（覆盖授悬浮窗权限后返回场景，因 `Settings.canDrawOverlays` 无 URI 可监听）。`App`（`Application` 子类）`onCreate` 调 `bind` 实现进程级注册，覆盖 App 在后台/未开 Activity 时用户切无障碍。`MainActivity` 订阅 Flow 刷新 UI（按钮三态：开无障碍/授悬浮窗/启动）。`OverlayService` 声明 `foregroundServiceType="specialUse"` + `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` + `FOREGROUND_SERVICE_SPECIAL_USE` 权限（Android 14）。`OverlayControllerMatchTest` 覆盖 `matchesEnabledService` 纯函数（null/空串/多条分隔/大小写/空格/前缀子串防误匹配）。

待 A/B 交付真实解析器后：A/B 调 `StoreRepository.push(StoreInfo)` 推真数据替换 fixtures；`FixtureProvider` 降级为离线兜底。

- **M2（完成，fixtures 驱动）**：`engine/store/StoreNameNormalizer`（NFKD 全角→半、去括号内容、分隔符归一、trim、lower）；`engine/match/ProductMatcher`（`NamePair`/`ItemMatch`、Levenshtein 相似度、贪心匹配、自动 ≥0.85 / 待确认 0.6–0.85 / 未配 <0.6 三档、确认对覆盖）；`engine/strategy/StrategyRecommender`（`Strategy`：最优平台 + reason + perPlatform，复用 `ActualPriceCalculator`）；`data/db` `ProductMatchEntity`+`ProductMatchDao`，AppDatabase v2 + `fallbackToDestructiveMigration`；`data/repo/MatchMemory`（`confirmed: StateFlow<Set<NamePair>>` + `confirm(...)` 落库）；`OverlayView` 改走 `StrategyRecommender` + 底部匹配概要（自动/待确认/未配），`OverlayService` 采 `MatchMemory.confirmed`。单测覆盖三个引擎（`StoreNameNormalizerTest`/`ProductMatcherTest`/`StrategyRecommenderTest`）。
- **M2 手动确认闭环（完成）**：`OverlayView` 待确认>0 时显示「确认 N 项匹配」按钮，`onConfirmPending` 回调；`OverlayService` 接到后对每对调 `MatchMemory.confirm(nameA,nameB,"meituan","flash")` → `confirmed` StateFlow 刷新 → 悬浮窗重算，待确认数清零。
- **M3 商家分析（完成，fixtures 驱动）**：`engine/SnapshotPricer`（纯函数 商品+包装+配送）+单测；`ItemPriceDao.findBySnapshot`、`StoreSnapshotDao.count`；`StoreRepository.historyFor(store,limit): Flow<List<HistoryPoint>>`（按快照重算参考价）+ `recordAll` + `seedIfEmpty`（首次空库播种 7 条带价格漂移的演示快照）；`ui/ChartView`（Canvas 折线+填充+高低标）；`ui/MerchantAnalysisView`（跨平台对比表 评分/月售/配送/起送 + `StrategyRecommender` reason + 历史图 + 「记录当前快照」按钮）；`MainActivity` 加「商家分析」入口切换视图，采集 `stores` 与 `historyFor` 两路 Flow。
- **M4 一键全采（完成，fixtures 假编排）**：`parsers/CollectionOrchestrator` 接口 + `CollectionState` 密封类（`InProgress`/`Completed`/`Failed`）；`data/repo/FakeCollectionOrchestrator`（fixtures + 延时模拟 meituan→flash 拉起/搜索/抓取）；`ui/CollectionView`（店名输入 + 「开始全采」+ 进度 + 结果，复用 `StrategyRecommender`）；`MainActivity` 加「一键全采（V2）」入口，完成时 `recordAll` 落库。真实手势/拉起为 A 的 `CollectionOrchestrator` 实现，C 只消费接口。
- **M5 打磨（完成）**：`parsers/SafeParse`（`try/catch` 包 `ParserInterface.parse`，失败返 null，A 调用即容错，悬浮窗显「暂无数据」而非崩）；`build.gradle.kts` `lint{}` 配置 + release 用 `signingConfig=debug` 便于侧载；Manifest 加 `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`；`MainActivity` 加「关闭电池优化」按钮；README 补「保活与 ROM 适配」（小米/华为/OPPO）与「构建 Release APK」说明。
