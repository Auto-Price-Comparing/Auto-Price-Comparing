# 外卖比价助手（Auto-Price-Comparing）

双平台（美团 / 淘宝闪购）本地比价 Android App。通过无障碍服务读取前台外卖 App 界面，悬浮窗显示跨平台实付价对比与最优策略。全部本地运行，无服务器。

## 技术栈

- Kotlin + 原生 Android（`minSdk 26`、`targetSdk 34`）
- Room（SQLite）历史库
- `AccessibilityService` 采集 + `WindowManager` 悬浮窗
- JUnit 单测

## 构建

> 本仓库暂未提交 Gradle wrapper jar。首次使用请用 Android Studio 打开项目根目录，IDE 会生成 `gradlew` 与 `gradle-wrapper.jar`（届时建议补提交）；或本机装 Gradle 8.x 后执行 `gradle wrapper`。

```bash
./gradlew assembleDebug      # 构建 debug APK
./gradlew installDebug       # 装机到已连接真机
./gradlew test               # 单测
./gradlew lint               # Android Lint
```

真机需：开发者选项 + USB 调试；授予悬浮窗权限；开启「外卖比价助手」无障碍服务。

## 模块与成员边界

详见 [`AGENTS.md`](./AGENTS.md)。简述：

| 成员 | 拥有 | 禁止触碰 |
|---|---|---|
| A | 采集框架 + 美团解析器 | `engine/`、`overlay/`、`ui/`、`data/` |
| B | 淘宝闪购解析器 + `AppLauncher.kt` | 同上 |
| C | `data/`、`engine/`、`overlay/`、`ui/`、构建配置 | `parsers/` 节点树代码 |

唯一对接面：`data/Models.kt` + `parsers/ParserInterface.kt`。

## 当前进度

- M0（完成）：C 脚手架
- M1 早期（完成，fixtures 驱动）：引擎单测、`StoreRepository`、悬浮窗 v2 对比卡、`OverlayController` 生命周期

详见 `AGENTS.md`「当前进度」段。

## 合规提示

仅限个人学习自用，侧载 APK，不传播；V2 自动采集绝不碰下单/支付流程。
