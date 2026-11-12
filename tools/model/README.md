# 校园楼栋 3D 模型（男生宿舍 6F）

沧州交通学院 男生宿舍板楼的程序化模型。几何只有一份，从同一份 Python 同时产出
**GLB**（给 App / 网页渲染）和 **.blend**（给你在 Blender 里改）。

## 产物

| 文件 | 说明 |
| --- | --- |
| `out/campus.blend` | **Blender 4.3 场景**，24 个命名对象 + 材质 + 灯光 + 3 台相机，打开即可改、F12 即可出图 |
| `out/campus.glb` | 同一份几何导出成 glTF 2.0（约 440 KB / 6.9k 面），给 App / three.js / Filament |
| `out/campus_preview.html` | 单文件离线预览（three.js + GLB 全内联），双击即开，断网可用 |
| `out/blender_render/*.png` | Blender EEVEE 渲染的鸟瞰 / 人视 / 正立面 |
| `app/src/main/assets/map/campus.html` | **App 里用的那一份**：单文件、离线、按 UniHub 配色重做 |

## 两条工作流

### A. 在 Blender 里改（推荐）

```
"D:/Blender 4.3/blender.exe" -b -P blender/build_campus.py     # 生成 out/campus.blend
```

或者直接双击打开 `out/campus.blend`：

- 大纲视图里三组：`主体楼`（Floor_1…6、Core_W/E、Roof）、`周边建筑`（Neighbor_01…14）、`场地`
- 相机 `Cam_鸟瞰 / Cam_人视 / Cam_正立面`，切换后 F12 出图
- 改尺寸：`Text Editor` → `build_campus.py` → 改顶部 `PARAMS` → `Run Script` 重建
  - `bays` 开间数 / `bay` 开间宽 / `depth` 进深 / `floors` 层数 / `h_first` `h_floor` 层高 / `win_w` `win_h` 窗洞
  - `BUILD_NEIGHBORS = False` 就不生成周边体块
- 场景里还嵌了 `README_场景说明.txt`

### B. 只更新网页 / App 用的 GLB

```
python gen_campus.py        # -> out/campus.glb
python make_preview.py      # -> out/campus_preview.html（桌面预览，深色 HUD）
python make_app_asset.py    # -> app/src/main/assets/map/campus.html（App 里用）
```

`app_template.html` 是 App 版本：底部四个胶囊按钮（周边 / 黄昏 / 环绕 / 鸟瞰），
浅色跟随 App 配色、支持 `?dark=1` 深色，相机按屏幕比例自动取景（投影二分，任何机型和横竖屏都框得住）。

## 模型约定

- 模型坐标系：**Y 轴向上，地面 y = 0**，单位米，楼体长轴沿 X，中心在原点。
  Blender 里是 Z-up，`build_campus.py` 里做了 `(x, y, z) -> (x, -z, y)` 的转换。
- 关键尺寸：66 × 15 m，一层 3.9 m + 标准层 3.3 m × 5 = 檐高 20.4 m；
  女儿墙 0.55 m + 屋顶柱廊 1.24 m + 压顶梁 0.20 m。
- 节点 / 对象命名：`Floor_1..6`、`Core_W`、`Core_E`、`Roof`、`Site`、`Neighbor_01..14`。
- 材质：`M_Wall_White` `M_Brick` `M_Brick_Dark` `M_Glass` `M_Frame` `M_Roof` `M_Slab`
  `M_Equip` `M_Concrete` `M_Ground` `M_Road` `M_Neighbor`(BLEND, α0.26)
- ⚠️ glTF 的 `baseColorFactor` 是**线性色**，脚本用 `s2l()` 从 sRGB 观感值转换；
  Blender 里材质也已经按线性写好了，改色直接用取色器。

## 立面依据（对照照片）

| 特征 | 做法 |
| --- | --- |
| 白色壁柱 + 竖向砖红窗洞 | 每个开间一片砖红墙板（窗宽 +0.6），两侧留白 |
| 白色楼层横带 | 每层楼面处一条 0.38 m 白带，压在砖红之上贯通整面 |
| 深灰平屋面 + **屋顶白色柱廊** | 女儿墙 0.55 + 方柱 0.34 × 0.34 @2.05 m + 一圈压顶梁 |
| 一层砖红基座 + 白雨篷入口 | 首层通高砖红，主入口在西侧第 4 开间 |
| 两端楼梯间 | 出挑 0.14 m、高 0.55 m，砖红带白腰带 + 小窗 |

## 预览页 URL 参数

- 桌面版：`?cam=x,y,z&tgt=x,y,z&neighbors=0&dusk=1`
- App 版：`?dark=1`

## 在 App 里的位置

`插件库（首页「获取插件」）→「实景地图」→ CampusMapActivity → WebView 加载 assets/map/campus.html`

## 照片来源

沧州交通学院（原北交大海滨学院）男生宿舍，5 张实景 / 航拍截图。
