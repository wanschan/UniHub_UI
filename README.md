<div align="center">

# UniHub · 校园通

**一个 100% 原生 Kotlin、零 Layout XML 的校园生活 App**

课程表 · NFC 打卡 · 扫码直达 · 校园圈子 · 订阅会员 · 邮箱验证码登录 · 3D 实景校区

![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?logo=kotlin&logoColor=white)
![minSdk](https://img.shields.io/badge/minSdk-24-3DDC84?logo=android&logoColor=white)
![targetSdk](https://img.shields.io/badge/targetSdk-37-3DDC84?logo=android&logoColor=white)
![No XML](https://img.shields.io/badge/Layout-XML%20Free-E8663A)
![version](https://img.shields.io/badge/version-26w46a-E8663A)
![License](https://img.shields.io/badge/License-未声明-lightgrey)

</div>

<p align="center">
  <a href="#功能清单">功能清单</a> ·
  <a href="#技术实现要点">技术实现要点</a> ·
  <a href="#快速开始">快速开始</a> ·
  <a href="#更新公告">更新公告</a> ·
  <a href="CHANGELOG.md">完整更新日志</a>
</p>

---

## 这是什么

UniHub 是一个校园生活类 App 的高保真实现。它先有一份纯 HTML/CSS 的交互原型（放在 [`design/`](design/)），再被逐屏、逐动效地翻译成**原生 Android 代码**。

与常见的「WebView 套壳」做法不同，除「实景地图」这一页要用 WebView 承载 three.js 的 3D 场景外，其余每一寸界面都是真实的 Android View：

- **没有一行 layout XML**——整棵 View 树在 Kotlin 里搭出来，尺寸、圆角、间距、阴影全部代码化
- **自定义 View 绘制**——开屏的墨色晕染、扫码框的往复扫描线、课表网格线都是 `onDraw` 画出来的
- **原生动效**——`ValueAnimator` + 三次贝塞尔缓动复刻原型的时间轴，不是 CSS transition 的近似
- **跟随系统的亮暗主题**——`values/` 与 `values-night/` 双份色板，切换后立即生效并持久化
- **真实登录流程**——邮箱收 6 位验证码才能进主界面，登录态落盘，重启免登录

共 **4 014 行 Kotlin / 17 个源文件**，无第三方 UI 库（除 Material Components 与 AppCompat），网络层只有 `HttpURLConnection`。

---

## 效果预览

<table>
  <tr>
    <td align="center"><b>开屏</b><br/><img src="screenshots/00-splash.png" width="200"/></td>
    <td align="center"><b>登录引导</b><br/><img src="screenshots/01-login.png" width="200"/></td>
    <td align="center"><b>首页 · 快捷启动</b><br/><img src="screenshots/02-home.png" width="200"/></td>
  </tr>
  <tr>
    <td align="center"><b>课表</b><br/><img src="screenshots/03-timetable.png" width="200"/></td>
    <td align="center"><b>圈子</b><br/><img src="screenshots/04-circles.png" width="200"/></td>
    <td align="center"><b>我</b><br/><img src="screenshots/05-me.png" width="200"/></td>
  </tr>
</table>

<table>
  <tr>
    <td align="center"><b>全局设置</b><br/><img src="screenshots/08-settings.png" width="200"/></td>
    <td align="center"><b>常见问题</b><br/><img src="screenshots/07-faq.png" width="200"/></td>
    <td align="center"><b>插件管理</b><br/><img src="screenshots/10-plugins.png" width="200"/></td>
  </tr>
  <tr>
    <td align="center"><b>赞助</b><br/><img src="screenshots/11-sponsor.png" width="200"/></td>
    <td align="center"><b>暗色 · 首页</b><br/><img src="screenshots/09-home-dark.png" width="200"/></td>
    <td align="center"><b>暗色 · 登录</b><br/><img src="screenshots/06-login-dark.png" width="200"/></td>
  </tr>
</table>

> 以上均为 1080 × 2400 模拟器实机截图，不是设计稿。

---

## 功能清单

### 开屏与登录

开屏动画（`LaunchActivity`）每次启动都播：

- 深灰底中央一枚白色圆盘，先做一次呼吸缩放
- 白盘以自身为圆心放大铺满整屏（约 700 ms，先慢后快）
- 转场后四角浮起墨色晕染，`Hello` 先淡入、再上浮至标题位（缩到 64sp，与登录页标题同字号）
- 停顿片刻后按登录态分流：已登录进主界面，未登录进登录页
- 点击屏幕任意处可跳过动画

登录（`LoginActivity`）只在未登录时出现，视觉上承接开屏的终点（白底、墨色晕染、顶部 `Hello`），所以转场是淡入而不是滑入：

- **两步式**：先填校园邮箱 → 再把收到的 6 位验证码填进去
- 验证码 `SecureRandom` 生成，5 分钟有效、60 秒重发冷却、最多输错 5 次
- 邮箱格式、验证码长度即时校验；出错时输入框横向抖动，底部给出剩余次数
- 验证码走 Brevo 事务邮件接口发送；debug 包在邮件发不出去时会把本次验证码直接显示在界面上，方便联调
- 登录成功写入本机（`AuthStore`），下次启动直接进主界面；「我」页可退出登录

### 首页

- 顶部 `Today` 标题与日期**固定不动**，只有下方「纸面」随导航左右平移
- 三个分段：快捷启动 / NFC 前往 / 扫码前往，切换时内容淡入、下划线指示器跟随
- 插件卡（课程、饮水）+「获取插件」半宽格子，点进插件管理弹层
- NFC 面板：大图标 + 三行状态信息
- 扫码面板：深色取景框 + 四角 L 形角标 + 往复扫描线 + 两条扫码历史

### 实景地图（3D 校区）

入口在「插件管理」顶部的「实景地图」卡片（已内置插件，不可关闭）。整页是 WebView 里的 three.js 场景，模型 GLB 已内联进 `assets/map/campus.html`，**约 1.3 MB、完全离线**，不需要任何网络请求：

- 拖动旋转、双指缩放；点击楼层高亮该层
- 底部四个开关：**周边**（显示/隐藏周边建筑）、**黄昏**（暖色天空氛围）、**环绕**（自动旋转）、**鸟瞰**（切到俯视机位）
- 跟随 App 主题：亮色 / 暗色各一套天空与地面配色
- 原生顶栏重绘，字号、描边、返回按钮与其它页面同源；模型解析期间显示「正在加载校园模型」，WebView 渲染进程崩溃有降级提示，不会白屏

### 课表

- 横向 7 天 × 纵向 8 节次的周视图网格
- 左侧节次时间轴（08:00 – 21:00），今天所在列加粗
- 课程块跨两行排布，按课程配色（6 组固定色板），显示课名、星标、地点
- 网格底线由自定义 `GridRow` 绘制

### 圈子

- 2 × 2 主题卡片：考研互助 / 二手书市 / 美食探店 / 电竞开黑
- 点击卡片筛选下方动态流，选中态显示描边，再点一次取消
- 动态卡：标签、相对时间、正文、点赞与评论计数

### 我

- 头像方块显示昵称首字 + `Hello, <昵称>`，标题下方带一行登录邮箱
- 菜单：全局设置（右侧实时显示当前主题与语言）、常见问题、更多（可展开：关于 / 隐私 / 联系）、赞助
- **退出登录**：二次确认后清空本机登录态并回到登录页，课表与主题设置不受影响
- 旋转 −9° 的订阅卡：49 RMB、欢迎订阅、详细信息入口

### 弹层（8 套内容共用一层底部抽屉）

| 弹层 | 内容 |
| --- | --- |
| 全局设置 | 语言 / 主题（亮 / 暗 / 跟随系统）分段控件、版本更新检测（读 `BuildConfig.VERSION_NAME`） |
| 常见问题 | 5 条手风琴，+/− 图标切换 |
| 关于 | 品牌 Logo、版本号与备案信息 |
| 隐私 | 个人信息收集清单 |
| 联系 | 客服邮箱，一键复制到剪贴板 |
| 赞助 | 6 / 12 / 28 三档金额选择 |
| 插件 | 顶部「实景地图」内置入口 + 5 个插件开关（课程、饮水、NFC、扫码、圈子） |
| 会员 | 权益列表 + 立即订阅 |

### 系统行为

- **主题**：亮 / 暗 / 跟随系统，写入 `SharedPreferences`，切换后 `recreate()` 立即生效
- **登录态**：开场页播完按 `AuthStore` 分流——已登录进主界面，未登录进登录页；`MainActivity` 启动时再兜底校验一次，未登录直接跳登录
- **转场**：开场页 → 登录页 / 主界面是 320 ms 淡入（`anim/route_in`），不用滑动，因为登录页视觉上就是开场页的延续
- **返回键**：先关弹层 → 再回首页 → 最后才退出，三级递进
- **状态栏**：真实系统栏 + `WindowInsets` 内边距，图标明暗随主题切换
- **Android 12+ 开屏**：`values-v31` 覆写 `windowSplashScreenAnimatedIcon`，系统闪屏与自绘开屏无缝衔接

---

## 设计系统

配色、圆角、字号全部从原型 1:1 迁移。

### 色彩

| 语义 | 亮色 | 暗色 |
| --- | --- | --- |
| 背景 `bg` | `#FFFFFF` | `#0D0F13` |
| 主文字 `ink` | `#111214` | `#F4F5F7` |
| 次级文字 `ink_2` | `#4A4D52` | `#A8ADB5` |
| 辅助文字 `ink_3` | `#9A9EA5` | `#6E747D` |
| 分隔线 `line` | `#E6E7EA` | `#262A31` |
| 卡片 `card` | `#F2F2F4` | `#171A1F` |
| 强调 `accent` | `#E8663A` | `#FF7A4D` |

另有 6 组固定色板用于课表与圈子标签：`blue / purple / sand / clay / sage / rose`，各自成对定义底色与文字色。

### 尺寸

| 项目 | 值 |
| --- | --- |
| 设计基准宽 | **392 dp**（所有值按屏宽等比缩放） |
| 页面左右留白 | 20 dp |
| 卡片圆角 | 14 / 15 / 16 / 18 / 22 dp |
| 底部抽屉圆角 | 26 dp（仅上圆角） |
| Tab 胶囊描边 | 1.6 dp |
| 卡片阴影 | elevation 2.5 dp |

### 动效曲线

统一使用 `PathInterpolator(0.22, 0.9, 0.28, 1.0)`；页面切换用 `(0.34, 0.72, 0.24, 1.0)`，时长 420 ms。

开屏时间轴（与原型对齐）：

```
0 ms      圆盘呼吸开始
790 ms    白盘放大铺满（1 → 18 倍）
1360 ms   墨色晕染浮现
1520 ms   Hello 淡入
2050 ms   Hello 上浮至顶部（缩到 64sp，与登录页标题同字号）
~2400 ms  停留片刻后按登录态分流：已登录 → 主界面，未登录 → 登录页
```

### 3D 校区配色

地图页不读 Android 资源，而是由 `campus.html` 里一份 CSS 变量驱动，主题通过 `?dark=0|1` 传入：

| 语义 | 亮色 | 暗色 | 黄昏（按钮切换） |
| --- | --- | --- | --- |
| 近天 `sky1` | `#FFFFFF` | `#1B1F26` | `#FFD7A6` |
| 中天 `sky2` | `#EEF2F6` | `#14171C` | `#E0A075` |
| 远天 `sky3` | `#DFE6EC` | `#0D0F13` | `#9D6F86` |
| 地面 `ground` | `#C9CFC4` | `#22262A` | 继承当前主题 |

楼体材质（`M_Wall_White` / `M_Brick` / `M_Glass` / `M_Roof` …）是三段式固定色，不随主题变化，只有天空与地面跟着走——这样夜景里楼还是那栋楼。

---

## 技术实现要点

**1. 全代码构建 UI**
没有 `setContentView(R.layout.xxx)`，`MainActivity` 里直接 new 出 `FrameLayout` / `LinearLayout` / `ScrollView`，再逐层 `addView`。好处是布局逻辑与状态强绑定、复用函数即复用样式；代价是没法用布局预览器。

**2. 等比缩放**
`Widgets.kt` 提供 `Context.sdp(v)`：以 392 dp 为基准，按 `widthPixels / (392 × density)` 换算。设计稿上的数字可以直接写进去，换到任何屏幕宽度都保持比例。

**3. 自定义 View**

| View | 职责 |
| --- | --- |
| `InkBlobs` | 四个 `RadialGradient` 色斑，按尺寸缓存 Shader，绘制时用 `Matrix` 做缓慢漂移 |
| `ScanBracket` | 四角 L 形取景标 + 往复扫描线 |
| `GridRow` | 课表横向网格底线 |

> 继承自 `LinearLayout` 一类的 ViewGroup 必须显式 `setWillNotDraw(false)`，否则 `onDraw` 不会被调用——这是本项目中踩过的一个坑。

**4. 主题切换**
`ThemePrefs` 封装 `SharedPreferences`（键 `cl-theme` / `cl-lang`），启动时 `AppCompatDelegate.setDefaultNightMode` 生效；用户在设置里改动后 `recreate()`，状态栏图标明暗通过 `WindowCompat.getInsetsController` 同步。

**5. 页面切换**
四个页面同时存在于一个 `FrameLayout` 中，切换时旧页面向左/右平移淡出、新页面自反方向进入，**标题层不动**，只有内容层（`PageShell.sheet`）参与位移——这是原型里最关键的一处手感。

**6. 底部抽屉**
`BottomSheetDialog` + 自定义 `SheetDialog` 样式（26 dp 上圆角、去掉最大宽度限制），内容由 `Sheets` 统一构建，8 套内容共用同一个容器与关闭逻辑。

**7. 邮箱验证码登录**

| 环节 | 做法 |
| --- | --- |
| 生成 | `SecureRandom` 取 6 位数字，`String.format(Locale.US, "%06d", …)` 补零 |
| 时效 | 5 分钟 TTL、60 秒重发冷却、5 次输错上限，超限必须重新获取 |
| 发送 | `BrevoMailer` 用 `HttpURLConnection` + `org.json` 直接打 Brevo 的 `POST /v3/smtp/email`，带 HTML 与纯文本两份正文 |
| 线程 | UI 线程只改状态，网络请求放在 `Executors.newSingleThreadExecutor()` |
| 保活 | 验证码只存内存；旋转屏幕时把 step / 邮箱 / 验证码 / 过期时间 / 已试次数写进 `onSaveInstanceState` 再取回 |
| 落盘 | 登录成功后 `AuthStore` 写 `SharedPreferences`（邮箱、昵称、方式、时间），`MainActivity` 启动时兜底校验 |

**8. WebView 里的 3D 校区**

`CampusMapActivity` 把 `file:///android_asset/map/campus.html?dark=0|1` 交给 WebView，页面内 three.js 用 `GLTFLoader` 解析内联的 GLB，`WebGLRenderer` 出图，`OrbitControls` 负责手势。主题通过 query 参数传入，页面读 `dark` 切换 CSS 变量与天空色，所以不需要重启 Activity 就能跟随主题。

几个必须处理的坑：`WebSettings` 要显式开 `domStorageEnabled`；`setAllowFileAccessFromFileURLs` 在 API 30+ 默认关闭，内联资源必须全部塞进同一个 HTML 才不会因跨域被拦；`onRenderProcessGone` 要返回 `true` 并给出降级提示，否则整个 App 会被拖崩。

**9. 密钥不进仓库**
Brevo 的 API Key 写在 `local.properties`（已被 `.gitignore` 排除），Gradle 侧读进来后用 `buildConfigField` 注入，Kotlin 只读 `BuildConfig.BREVO_API_KEY`。仓库里搜不到真实 Key，别人 clone 下来该值为空，App 会提示「未配置 Brevo API Key」而不是崩溃。

> 但这只是「不把 Key 公开」，不是「Key 安全」——它仍然会被打进 APK。上线前必须把发信搬到服务端。

---

## 项目结构

```
UniHub_UI/
├── app/
│   └── src/main/
│       ├── java/com/example/applicationoneapp/
│       │   ├── MainActivity.kt            # 主容器：四页叠加 + TabBar + 弹层 + 返回栈
│       │   ├── auth/
│       │   │   ├── AuthConfig.kt          # 验证码参数（API Key 由 BuildConfig 注入）
│       │   │   ├── AuthStore.kt           # 登录态持久化 + 昵称推导
│       │   │   └── BrevoMailer.kt         # 调 Brevo 事务邮件发验证码
│       │   ├── common/
│       │   │   ├── Pal.kt                 # 颜色取值（主题感知）
│       │   │   ├── ThemePrefs.kt          # 主题 / 语言持久化
│       │   │   └── Ui.kt                  # dp、圆角、颜色、字体等基础工具
│       │   ├── ui/
│       │   │   ├── LaunchActivity.kt      # 开场动画 + 按登录态分流
│       │   │   ├── LoginActivity.kt       # 邮箱 + 6 位验证码
│       │   │   ├── CampusMapActivity.kt   # WebView 承载 3D 校区
│       │   │   ├── page/
│       │   │   │   ├── HomePage.kt        # 首页（含 ScanBracket）
│       │   │   │   ├── TimetablePage.kt   # 课表（含 GridRow）
│       │   │   │   ├── CirclesPage.kt     # 圈子
│       │   │   │   └── MePage.kt          # 我
│       │   │   ├── sheet/Sheets.kt        # 8 套底部弹层
│       │   │   └── widget/
│       │   │       ├── Widgets.kt         # PageShell / TabBar / Toast / 缩放适配
│       │   │       └── InkBlobs.kt        # 墨色晕染色斑
│       ├── assets/map/campus.html         # 内联 GLB 的 three.js 校区（离线单文件）
│       ├── res/
│       │   ├── values/                    # 亮色色板、字体、主题
│       │   ├── values-night/              # 暗色色板、主题
│       │   ├── values-v31/                # Android 12+ 系统闪屏
│       │   ├── anim/                      # 登录页淡入转场
│       │   ├── drawable/                  # 手绘矢量图标
│       │   ├── drawable-*dpi/             # 品牌位图（Logo / 闪屏圆盘）
│       │   └── mipmap-*dpi/               # 启动器图标（脚本出图）
│       └── AndroidManifest.xml
├── design/                                # 原始 HTML 交互原型
│   ├── index.html                         # 主界面
│   └── login.html                         # 登录引导
├── screenshots/                           # 实机截图
├── tools/
│   ├── make_icons.py                      # 源图 → 各密度启动器图标 / 闪屏
│   ├── preview_icons.py                   # 生成遮罩与闪屏效果预览
│   └── model/                             # 3D 校区模型流水线（详见其 README）
│       ├── blender/build_campus.py        # 程序化建模 → .blend
│       ├── gen_campus.py                  # 同一份几何 → .glb
│       └── make_app_asset.py              # → assets/map/campus.html
├── CHANGELOG.md                           # 更新日志（26wNNx 编号规则）
├── gradle/libs.versions.toml              # 版本目录
└── settings.gradle.kts
```

---

## 快速开始

### 环境要求

| 项目 | 版本 |
| --- | --- |
| JDK | **25**（AGP 9.4 的 toolchain 要求，低版本会构建失败） |
| Gradle | 9.7.0（随 wrapper 提供，无需单独安装） |
| AGP | 9.4.0 |
| compileSdk / targetSdk | 37 |
| minSdk | 24（Android 7.0） |
| Android Studio | Ladybug 及以上（需支持 AGP 9.4） |

> ⚠️ 如果本机默认 JDK 是 11 或 17，请在 `Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK` 里显式指定 JDK 25，否则会报 `Unable to download toolchain matching {languageVersion=25}`。

### 命令行构建

```bash
# macOS / Linux
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

安装到设备：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

首次启动进入开屏动画 → 登录页，填邮箱收验证码登录后进主界面。**没有配置邮件服务时登录不可用**，按下节配一下即可。

### 配置登录验证码（必做）

验证码通过 [Brevo](https://www.brevo.com/) 的事务邮件接口发送，凭据不写进源码。在项目根目录的 `local.properties`（该文件不入库）追加：

```properties
# Brevo 后台 → SMTP & API → API Keys 新建，权限只要 Transactional email
brevo.apiKey=xkeysib-xxxxxxxxxxxxxxxx
# 必须是 Brevo 里已验证的发件人，否则接口报 400 sender not valid
brevo.senderEmail=you@your-domain.com
brevo.senderName=UniHub
```

不配也能装能跑：`BuildConfig.BREVO_API_KEY` 为空时，点「获取验证码」会提示「未配置 Brevo API Key」；debug 包还会把本次生成的验证码直接印在错误提示里，方便只调界面时用。

> 工程里没有、也不该有真实 Key。用环境变量 `BREVO_API_KEY` 也能注入，适合 CI。

### 品牌图标

启动器图标 / 系统闪屏 / 自绘开屏圆盘上的 Logo 全部由同一张源图生成，换图标只需重跑：

```bash
python tools/make_icons.py      # 生成 mipmap-* 与 drawable-* 下各密度 PNG
python tools/preview_icons.py   # 生成遮罩/闪屏效果预览（build/icon-preview/）
```

源图路径写在 `make_icons.py` 的 `SRC`；脚本会抠掉白底、按各场景的安全区比例居中（自适应图标 60%、传统图标 86%、闪屏白盘 50%、开屏圆盘 62%）。

### 3D 校区模型

`assets/map/campus.html` 不是手改的，由 `tools/model/` 下的流水线产出：同一份 Python 几何先导出 `.glb`，再由 `make_app_asset.py` 把 GLB、three.js、交互脚本内联成单个离线 HTML。

```bash
cd tools/model
python gen_campus.py        # -> out/campus.glb
python make_app_asset.py    # -> ../../app/src/main/assets/map/campus.html
```

要在 Blender 里改模型（开间数、层数、层高、窗洞、周边体块）见 [`tools/model/README.md`](tools/model/README.md)。

### 关于 Gradle 下载源

`gradle/wrapper/gradle-wrapper.properties` 里的 `distributionUrl` 指向阿里云镜像，国内网络更稳。若在海外，改成官方地址即可：

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.7.0-bin.zip
```

---

## HTML 原型

[`design/`](design/) 下是最初的交互原型，纯 HTML + CSS + 原生 JS，单文件、零依赖：

| 页面 | 文件 |
| --- | --- |
| 登录引导 | [`design/login.html`](design/login.html) |
| 主界面 | [`design/index.html`](design/index.html) |

双击即可在浏览器打开。桌面端以 392 × 844 的手机外框居中展示，窗口宽度小于 470 px 时自动全屏；支持锚点直达 `#home` / `#timetable` / `#circles` / `#me`。

原生实现与原型的差异，主要在「iOS 风格的模拟状态栏」被替换成了真实系统状态栏 + `WindowInsets`。

---

## 已知限制

- **数据仍以本地 Mock 为主**：课程、动态、扫码历史写死在代码里，没有数据库；只有登录会真的发邮件
- **登录是纯客户端实现**：验证码在本机生成、本机校验，登录态只存在本机 `SharedPreferences`，换设备 / 清数据需要重新验证；没有服务端签发的 token
- **Brevo Key 仍在 APK 里**：虽然源码与仓库不含真实 Key，但运行时它必然被打进包里，能被反编译提取。上线前应把「生成 + 发送验证码」搬到服务端
- **包名仍为占位符** `com.example.applicationoneapp`，正式发布前需重命名
- **仅中文单语种**：`lint` 中已关闭 `HardcodedText` / `SetTextI18n` / `MissingTranslation`
- **未适配横屏与平板**：布局按 392 dp 竖屏等比缩放，横屏会出现大量留白（3D 地图已按屏幕比例自动取景）
- **无单元测试与 UI 测试**：`androidTest` 目录仅有模板文件
- **3D 地图首次加载有 1–2 秒解析耗时**，且使 APK 体积增加约 1.1 MB

## Roadmap

- [ ] 服务端签发 token，验证码改由后端生成与校验
- [ ] 接入真实课表数据（教务系统 / JSON 导入）
- [ ] 「饮水」做成可记账的消费流水页
- [ ] NFC 与扫码接入真实能力（`NfcAdapter` / CameraX）
- [ ] 圈子接入后端，支持发帖与评论
- [ ] 3D 校区接入室内导航：点房间跳楼层并给出路线
- [ ] 抽取 `strings.xml` 支持多语言
- [ ] 平板 / 折叠屏布局

---

## 更新公告

### 26w46a · 2026-11-12

本次更新把「点一下就能进的 Demo」做成了真正需要验证身份的 App，并把校园搬进了 3D。

**新增**

- 邮箱验证码登录：两步式（邮箱 → 6 位码），5 分钟有效、60 秒重发、5 次错误上限
- 登录态本地持久化，重启免登录；「我」页新增退出登录，头像与昵称来自真实账号
- 实景地图：插件库新增入口，three.js 渲染的 3D 校区，可旋转缩放、点楼层高亮，支持周边 / 黄昏 / 环绕 / 鸟瞰四个开关，完全离线
- 品牌位图与启动器图标改由脚本统一产出，新增图标预览工具

**改进**

- 品牌从 CampusLink 统一为 UniHub（主题名、关于、客服邮箱、应用名）
- 启动流程拆分：开场动画与登录页各司其职，转场改为淡入以承接墨色衔接
- 关于弹层换用真实 Logo

**安全**

- Brevo API Key 移出源码，改由不入库的 `local.properties` 注入 `BuildConfig`

完整变更见 [`CHANGELOG.md`](CHANGELOG.md)。版本号规则：`年数 + w + ISO 周数 + 更新序`，例如 `26w46a` 表示 2026 年第 46 周的第 1 次更新。

---

## 许可证

本项目暂未声明开源许可证。如需商用或二次分发，请先联系作者。

<div align="center">
<sub>Designed & built by <a href="https://github.com/wanschan">wanschan</a></sub>
</div>
