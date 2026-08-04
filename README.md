# LocLogger

LocLogger 是一款开源的 Android GPS 轨迹记录应用，基于 Kotlin 与 Jetpack Compose 构建。支持后台持续记录轨迹、多源地图展示、轨迹批注，以及 GPX / KML / KMZ / TXT / CSV 多种格式导出与本地备份恢复。所有数据仅保存在本地，无云端依赖。

包名：`moe.telecom.loclogger`

## 功能特性

- **实时定位仪表盘**：纬度、经度、高度、速度、精度、方向、卫星数
- **轨迹记录**：开始 / 暂停 / 继续 / 停止，前台服务持续记录，支持开机自启
- **多源地图**：基于 osmdroid，支持 OSM / Google / 高德等瓦片源
- **轨迹批注**：记录过程中随时添加文字批注，导出为路点
- **轨迹管理**：按活动类型分类（步行 / 跑步 / 骑行 / 驾车 / 公交 / 船）
- **数据导出**：GPX（1.0 / 1.1 / 2.2）、KML、KMZ、TXT、CSV
- **备份恢复**：本地备份与恢复
- **丰富设置**：GPS 更新周期、时间 / 距离过滤、EGM96 高度修正、坐标格式（度分秒 / 十进制度）、单位制（公制 / 英制）、方向显示
- **主题**：Material 3 / MIUI X（Liquid Glass）/ 经典红，支持动态取色（Material Kolor）
- **本地存储**：Room + DataStore，数据不离开设备

## 技术栈

| 类别 | 技术 |
| --- | --- |
| 语言 | Kotlin |
| UI | Jetpack Compose、Material 3、Miuix KMP |
| 架构 | MVVM + Hilt 依赖注入 |
| 数据 | Room、DataStore Preferences |
| 地图 | osmdroid |
| 定位 | Google Play Services Location |
| 序列化 | kotlinx.serialization |

## 项目结构

| 目录 | 职责 |
| --- | --- |
| `app/src/main/java/moe/telecom/loclogger/` | 应用源码 |
| `.../data/` | 本地数据库（Room）、仓库、后台服务与轨迹导出 |
| `.../di/` | Hilt 依赖注入模块 |
| `.../domain/` | 领域模型与用例 |
| `.../ui/` | Compose UI（页面、组件、主题、权限） |
| `.../viewmodel/` | ViewModel 层 |
| `gradle/` | Gradle Wrapper 与版本目录（`libs.versions.toml`） |
| `.github/workflows/` | GitHub Actions 自动构建 |

## 构建与配置

### 本地开发

1. 安装 JDK 17+ 与 Android SDK（compileSdk 37）。
2. 使用 Android Studio 打开仓库根目录，或命令行执行：

   ```bash
   ./gradlew :app:assembleDebug
   ```

3. Debug APK 输出：`app/build/outputs/apk/debug/app-debug.apk`。

Release 签名密钥库已提交在仓库 `keystore/loclogger-release.keystore`；密码不写入仓库，本地读取根目录 `keystore.properties`（已被 gitignore），CI 通过 GitHub Secrets 注入。

### GitHub Actions 自动构建

- `.github/workflows/debug.yml`：任意分支推送或手动触发，构建 Debug APK（Beta 构建）。
- `.github/workflows/release.yml`：推送 `v*` 或 `x.y.z` 版本标签触发，构建签名的 Release APK 与 AAB，并发布到 GitHub Release。

Release 构建所需 Secrets：

```text
LOCLOGGER_RELEASE_STORE_PASSWORD    # keystore 密码
LOCLOGGER_RELEASE_KEY_ALIAS         # 签名 key 别名（默认 loclogger）
LOCLOGGER_RELEASE_KEY_PASSWORD      # 签名 key 密码
```

构建时由 Actions 将 Secrets 以 `ORG_GRADLE_PROJECT_*` 注入 Gradle，签名密钥库直接使用仓库内的 `keystore/loclogger-release.keystore`。

本地 Release 签名：在仓库根目录创建 `keystore.properties`（已被 gitignore），键名同上三个 `LOCLOGGER_RELEASE_*`，执行 `./gradlew :app:assembleRelease` 即可生成签名 APK。

## 开源许可

本项目暂未添加 LICENSE 文件。
