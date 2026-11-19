# 打大A 记牌器

原生 Android（Kotlin + Jetpack Compose）打大A 记牌器（108张/5人），具备手动记牌、屏幕截图 + ML Kit 自动识别、悬浮窗、按玩家记牌、4取蛋子等功能。

## 功能
- 打大A 记牌（108张 / 5人）
- 手动记牌 + 剩余牌面板
- 屏幕截图 + ML Kit 文字识别 + 跨帧去重
- 按位置自动归属玩家 + 玩家剩余手牌显示
- 4取蛋子（4 的剩余数减少，被取点数自动增加）
- 悬浮窗（圆形图标 / 展开横屏长条 / 可拖动 / 消失5秒）
- ROI 框选 + 识别参数调参

## 环境要求
- Android Studio（最新稳定版）
- JDK 17（Android Studio 自带）
- Android SDK Platform 35
- 磁盘空间 ≥ 10 GB

## 电脑构建 APK

1. 解压本工程，得到 `CardCounter` 文件夹
2. 启动 Android Studio → Open → 选择 `CardCounter` 文件夹
3. 弹窗点 Trust，等待右下角 Gradle 同步完成（首次需联网下载依赖，约 10~30 分钟）
4. 菜单栏 `Build → Build App Bundle(s) / APK(s) → Build APK(s)`
5. APK 产物：`app/build/outputs/apk/debug/app-debug.apk`
6. 将 APK 传到手机安装（需允许未知来源）

## 国内网络加速（可选）

把根目录 `settings.gradle.kts` 的仓库配置替换为：

```kotlin
pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/central")
        google()
        mavenCentral()
    }
}
rootProject.name = "CardCounter"
include(":app")
```

## 命令行构建（备选）

```bash
# 生成 wrapper（Android Studio Gradle 面板运行 wrapper 任务后可用）
./gradlew assembleDebug        # macOS / Linux
gradlew.bat assembleDebug      # Windows
```

## 手机云端构建（GitHub Actions，无需电脑）

1. 手机浏览器注册并登录 GitHub（github.com）
2. 新建仓库（New repository），名字随意（如 CardCounter）
3. 点 “Add file → Upload files”，把解压后的整个工程文件上传上去（含 .github 目录）
4. 上传后，点仓库顶部 **Actions** 标签
5. 点左侧 **Build APK** → 右侧 **Run workflow** → 确认运行
6. 等几分钟构建完成，进入该次运行，底部 **Artifacts** 里下载 `app-debug`（解压后得到 app-debug.apk）
7. 安装到手机即可

## 使用说明

1. 首页点击“打大A”进入牌局页
2. 确定大A后，点击【开始】按钮开始记牌
3. 手动记牌：选择出牌玩家（本/上/对/下/五），点击剩余牌点数
4. 4取蛋子：点“4取蛋子”选择4的个数和被取点数
5. 自动识别：点“开始自动识别” → 授予悬浮窗权限 → 允许录屏，自动识别并按位置归玩家
6. 悬浮窗：圆形图标点击展开；【设置】里可缩小/消失5秒
7. 识别区域：识别中点“设置识别区域”框选牌面范围

## 目录结构

```
app/src/main/java/com/example/cardcounter/
├── data/        # 模型、牌池、记牌引擎
├── core/        # 会话状态、最新帧缓存
├── capture/     # ML Kit 识别、去重、录屏服务、玩家区域、ROI/调参
├── overlay/     # 悬浮窗
└── ui/          # 首页、牌局页、ROI框选、识别设置
```
