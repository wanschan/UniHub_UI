# -*- coding: utf-8 -*-
"""
沧州交通学院 · 6 层男生宿舍（主体楼）+ 周边半透明体块 —— 程序化建模

不依赖 Blender：直接按照 glTF 2.0 规范写二进制 GLB。
坐标系：Y 轴向上，地面 y = 0，单位：米。
主体楼长轴沿 X，短轴沿 Z，中心位于原点。

楼体特征（依据 5 张实景/航拍照片）：
  · 6 层板式宿舍，长约 66m、进深 15m，一层层高较高（3.9m），标准层 3.3m
  · 白色涂料墙面 + 砖红色水平腰线 + 窗下砖红墙裙 -> 远看是"白色壁柱夹红色窗洞"
  · 屋顶深灰平屋面，白色女儿墙，女儿墙上有一圈白色立柱（本楼最显眼的特征）
  · 一层砖红基座，主入口带白色雨篷，玻璃门
  · 两端各有一个略出挑的砖红楼梯间体块

输出：
  out/campus.glb          —— 可被 Android(Filament/WebView+three.js) 直接加载
  out/preview.html        —— 单文件离线预览（three.js 内联）
"""
import json
import math
import os
import struct

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "out")

# ----------------------------------------------------------------------------
# 材质        name, baseColor(r,g,b,a), metallic, roughness, alphaMode
# ----------------------------------------------------------------------------
MATERIALS = [
    ("M_Wall_White",  (0.905, 0.892, 0.858, 1.0), 0.00, 0.82, "OPAQUE"),
    ("M_Brick",       (0.386, 0.163, 0.140, 1.0), 0.00, 0.88, "OPAQUE"),
    ("M_Brick_Dark",  (0.286, 0.118, 0.104, 1.0), 0.00, 0.90, "OPAQUE"),
    ("M_Glass",       (0.145, 0.190, 0.220, 1.0), 0.30, 0.07, "OPAQUE"),
    ("M_Frame",       (0.878, 0.878, 0.858, 1.0), 0.00, 0.55, "OPAQUE"),
    ("M_Roof",        (0.243, 0.250, 0.258, 1.0), 0.05, 0.86, "OPAQUE"),
    ("M_Slab",        (0.760, 0.745, 0.715, 1.0), 0.00, 0.86, "OPAQUE"),
    ("M_Equip",       (0.545, 0.560, 0.548, 1.0), 0.18, 0.70, "OPAQUE"),
    ("M_Concrete",    (0.632, 0.622, 0.596, 1.0), 0.00, 0.92, "OPAQUE"),
    ("M_Ground",      (0.452, 0.508, 0.428, 1.0), 0.00, 0.96, "OPAQUE"),
    ("M_Road",        (0.548, 0.548, 0.532, 1.0), 0.00, 0.96, "OPAQUE"),
    ("M_Neighbor",    (0.600, 0.690, 0.790, 0.26), 0.00, 0.60, "BLEND"),
]
MAT_INDEX = {m[0]: i for i, m in enumerate(MATERIALS)}


# ----------------------------------------------------------------------------
# 几何累加器：按材质分桶
# ----------------------------------------------------------------------------
class Mesh:
    def __init__(self):
        self.buckets = {}   # matName -> [pos[], nor[], idx[]]

    def _b(self, mat):
        return self.buckets.setdefault(mat, [[], [], []])

    def quad(self, mat, p0, p1, p2, p3):
        """四点逆时针（从外侧看即是逆时针）"""
        pos, nor, idx = self._b(mat)
        ax, ay, az = p1[0] - p0[0], p1[1] - p0[1], p1[2] - p0[2]
        bx, by, bz = p2[0] - p0[0], p2[1] - p0[1], p2[2] - p0[2]
        nx, ny, nz = ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx
        ln = math.sqrt(nx * nx + ny * ny + nz * nz) or 1.0
        nx, ny, nz = nx / ln, ny / ln, nz / ln
        base = len(pos)
        for p in (p0, p1, p2, p3):
            pos.append((p[0], p[1], p[2]))
            nor.append((nx, ny, nz))
        idx.extend([base, base + 1, base + 2, base, base + 2, base + 3])

    def box(self, mat, x0, y0, z0, x1, y1, z1, skip=()):
        if x1 < x0: x0, x1 = x1, x0
        if y1 < y0: y0, y1 = y1, y0
        if z1 < z0: z0, z1 = z1, z0
        if "px" not in skip:
            self.quad(mat, (x1, y0, z1), (x1, y0, z0), (x1, y1, z0), (x1, y1, z1))
        if "nx" not in skip:
            self.quad(mat, (x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0))
        if "py" not in skip:
            self.quad(mat, (x0, y1, z0), (x0, y1, z1), (x1, y1, z1), (x1, y1, z0))
        if "ny" not in skip:
            self.quad(mat, (x0, y0, z1), (x0, y0, z0), (x1, y0, z0), (x1, y0, z1))
        if "pz" not in skip:
            self.quad(mat, (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1))
        if "nz" not in skip:
            self.quad(mat, (x1, y0, z0), (x0, y0, z0), (x0, y1, z0), (x1, y1, z0))

    def quad_y(self, mat, x0, z0, x1, z1, y):
        """水平面（朝上）"""
        self.quad(mat, (x0, y, z0), (x0, y, z1), (x1, y, z1), (x1, y, z0))


# ----------------------------------------------------------------------------
# 主体楼参数（想改尺寸就改 DEFAULT_PARAMS，或调 configure(**kwargs)）
# ----------------------------------------------------------------------------
DEFAULT_PARAMS = dict(
    bays=15,            # 开间数
    bay=4.4,            # 开间宽
    depth=15.0,         # 进深
    floors=6,           # 层数
    h_first=3.9,        # 一层层高
    h_floor=3.3,        # 标准层高
    win_w=2.55,         # 窗宽
    win_h=1.55,         # 窗高
    win_sill=0.95,      # 窗台距楼面
    entrance_bay=3,     # 主入口开间
    back_door_bay=10,   # 次要出入口开间
)

BAYS = BAY = L = D = F1 = FH = NFL = H = 0
PARAPET = 0.55              # 女儿墙高（柱廊坐在上面）
COL_H = 1.24                # 屋顶柱廊柱高
POST_H = 0.0               # 女儿墙立柱高出部分
X0 = X1 = Z0 = Z1 = 0.0
WALL_T = 0.36               # 外墙厚
WIN_W = WIN_H = WIN_SILL = 0.0
ENTRANCE_BAY = BACK_DOOR_BAY = 0


def configure(**kw):
    """改参数并重算派生量。已生成的几何不受影响，需重建。"""
    global BAYS, BAY, L, D, F1, FH, NFL, H, X0, X1, Z0, Z1
    global WIN_W, WIN_H, WIN_SILL, ENTRANCE_BAY, BACK_DOOR_BAY
    p = dict(DEFAULT_PARAMS)
    p.update({k: v for k, v in kw.items() if k in p})
    BAYS = int(p["bays"])
    BAY = float(p["bay"])
    D = float(p["depth"])
    NFL = int(p["floors"])
    F1 = float(p["h_first"])
    FH = float(p["h_floor"])
    WIN_W = float(p["win_w"])
    WIN_H = float(p["win_h"])
    WIN_SILL = float(p["win_sill"])
    ENTRANCE_BAY = max(0, min(BAYS - 1, int(p["entrance_bay"])))
    BACK_DOOR_BAY = max(0, min(BAYS - 1, int(p["back_door_bay"])))
    L = BAYS * BAY
    H = F1 + FH * (NFL - 1)
    X0, X1 = -L / 2, L / 2
    Z0, Z1 = -D / 2, D / 2
    return p


configure()


def floor_base(k):
    """第 k 层（1-based）楼面标高"""
    return 0.0 if k == 1 else F1 + FH * (k - 2)


def face_quad(mesh, mat, side, x0, y0, x1, y1, off):
    """在长边立面上贴一片四边形；off>0 表示向室外偏移"""
    if side == "S":
        z = Z0 - off
        mesh.quad(mat, (x1, y0, z), (x0, y0, z), (x0, y1, z), (x1, y1, z))
    else:
        z = Z1 + off
        mesh.quad(mat, (x0, y0, z), (x1, y0, z), (x1, y1, z), (x0, y1, z))


def add_window(mesh, side, cx, y0, w=None, h=None, mullions=2):
    """窗：白色窗套(后面) + 玻璃(前面) + 竖向棂 + 窗台挑出"""
    w = WIN_W if w is None else w
    h = WIN_H if h is None else h
    fw, fh = w + 0.26, h + 0.30
    face_quad(mesh, "M_Frame", side, cx - fw / 2, y0 - 0.15, cx + fw / 2, y0 - 0.15 + fh, 0.020)
    face_quad(mesh, "M_Glass", side, cx - w / 2, y0, cx + w / 2, y0 + h, 0.055)
    # 竖向窗棂
    for i in range(mullions + 1):
        t = -w / 2 + w * i / mullions
        face_quad(mesh, "M_Frame", side, cx + t - 0.028, y0, cx + t + 0.028, y0 + h, 0.070)
    # 横向中挺
    face_quad(mesh, "M_Frame", side, cx - w / 2, y0 + h * 0.62, cx + w / 2, y0 + h * 0.62 + 0.05, 0.068)
    # 窗台（向外挑 90mm）
    z_out = (Z0 - 0.09) if side == "S" else (Z1 + 0.09)
    z_in = Z0 if side == "S" else Z1
    mesh.box("M_Frame", cx - fw / 2 - 0.04, y0 - 0.30, min(z_in, z_out),
             cx + fw / 2 + 0.04, y0 - 0.16, max(z_in, z_out))


def build_floor(k):
    """单层：楼板 + 四面外墙 + 中间走廊墙 + 立面装饰 + 窗"""
    m = Mesh()
    y0 = floor_base(k)
    yt = y0 + (F1 if k == 1 else FH)

    # 楼板（下层顶板）
    m.box("M_Slab", X0, y0 - 0.20, Z0, X1, y0, Z1)
    # 外墙环（做成空腔，拆层时能看到楼层平面）
    m.box("M_Wall_White", X0, y0, Z0, X1, yt, Z0 + WALL_T)
    m.box("M_Wall_White", X0, y0, Z1 - WALL_T, X1, yt, Z1)
    m.box("M_Wall_White", X0, y0, Z0, X0 + WALL_T, yt, Z1)
    m.box("M_Wall_White", X1 - WALL_T, y0, Z0, X1, yt, Z1)
    # 中间走廊隔墙
    m.box("M_Slab", X0 + WALL_T, y0, -0.09, X1 - WALL_T, yt, 0.09)

    for side in ("S", "N"):
        # 一层整体砖红基座
        if k == 1:
            face_quad(m, "M_Brick", side, X0, y0, X1, y0 + F1, 0.014)

        # 每个开间：一片竖向砖红窗洞 + 一樘窗（砖红两侧让出白色壁柱）
        for b in range(BAYS):
            bx0 = X0 + b * BAY
            cx = bx0 + BAY / 2
            wy = y0 + (WIN_SILL if k > 1 else 1.15)

            if k == 1 and side == "S" and b == ENTRANCE_BAY:
                continue
            if k == 1 and side == "N" and b == BACK_DOOR_BAY:
                continue

            if k > 1:
                panel_w = WIN_W + 0.60
                off_p = (BAY - panel_w) / 2
                face_quad(m, "M_Brick", side, bx0 + off_p, y0, bx0 + BAY - off_p, yt, 0.014)

            add_window(m, side, cx, wy, WIN_W, WIN_H, 2)

        # 白色楼层横带（压在砖红之上，贯通整面）
        if k > 1:
            face_quad(m, "M_Wall_White", side, X0, y0 - 0.04, X1, y0 + 0.34, 0.030)

    # 一层入口
    if k == 1:
        build_entrance(m, X0 + (ENTRANCE_BAY + 0.5) * BAY, "S")
        build_entrance(m, X0 + (BACK_DOOR_BAY + 0.5) * BAY, "N", back=True)
    return m


def build_entrance(m, cx, side, back=False):
    """白色雨篷 + 玻璃门 + 台阶"""
    z_face = Z0 if side == "S" else Z1
    sgn = -1 if side == "S" else 1
    w = 7.4
    # 门洞（玻璃）
    face_quad(m, "M_Glass", side, cx - w / 2 + 0.6, 0.10, cx + w / 2 - 0.6, 3.05, 0.055)
    # 门框竖挺
    for i in range(-1, 2):
        face_quad(m, "M_Frame", side, cx + i * (w / 2 - 0.6) - 0.05, 0.10,
                  cx + i * (w / 2 - 0.6) + 0.05, 3.05, 0.075)
    face_quad(m, "M_Frame", side, cx - w / 2 + 0.55, 3.05, cx + w / 2 - 0.55, 3.18, 0.075)
    # 门套（砖红）
    face_quad(m, "M_Brick_Dark", side, cx - w / 2, 0.0, cx + w / 2, 3.35, 0.030)
    # 雨篷
    depth = 3.6 if not back else 2.6
    z_out = z_face + sgn * depth
    m.box("M_Frame", cx - w / 2 - 0.3, 3.30, min(z_face, z_out),
          cx + w / 2 + 0.3, 3.66, max(z_face, z_out))
    # 雨篷下两条细柱
    for sx in (-1, 1):
        px = cx + sx * (w / 2 - 0.45)
        m.box("M_Frame", px - 0.11, 0.0, z_out + sgn * 0.28,
              px + 0.11, 3.30, z_out + sgn * 0.06)
    # 台阶
    for i, dy in enumerate((0.45, 0.30, 0.15)):
        dd = depth + 0.4 + i * 0.55
        m.box("M_Concrete", cx - w / 2 - 0.5, 0.0, min(z_face, z_face + sgn * dd),
              cx + w / 2 + 0.5, dy, max(z_face, z_face + sgn * dd))


def build_core(side):
    """两端楼梯间体块（砖红，略出挑、略高）"""
    m = Mesh()
    w = 5.2
    x0, x1 = (X0 - 0.14, X0 + w) if side == "W" else (X1 - w, X1 + 0.14)
    z0, z1 = Z0 - 0.85, Z1 + 0.85
    m.box("M_Brick", x0, 0.0, z0, x1, H + 0.55, z1)
    # 每层白色腰带
    for k in range(1, NFL):
        y = floor_base(k)
        m.box("M_Wall_White", x0 - 0.06, y - 0.22, z0 - 0.05, x1 + 0.06, y + 0.02, z1 + 0.05)
    # 顶部白檐
    m.box("M_Frame", x0 - 0.18, H + 0.55, z0 - 0.18, x1 + 0.18, H + 0.95, z1 + 0.18)
    # 山墙上的小窗
    for k in range(2, NFL + 1):
        y = floor_base(k) + 1.05
        zc = (z0 + z1) / 2
        nx = -1 if side == "W" else 1
        if side == "W":
            fx = x0 - 0.02
            m.quad("M_Frame", (fx, y - 0.14, zc - 0.9), (fx, y - 0.14, zc + 0.9),
                   (fx, y + 1.5, zc + 0.9), (fx, y + 1.5, zc - 0.9))
            m.quad("M_Glass", (fx - 0.03, y, zc - 0.72), (fx - 0.03, y, zc + 0.72),
                   (fx - 0.03, y + 1.32, zc + 0.72), (fx - 0.03, y + 1.32, zc - 0.72))
        else:
            fx = x1 + 0.02
            m.quad("M_Frame", (fx, y - 0.14, zc + 0.9), (fx, y - 0.14, zc - 0.9),
                   (fx, y + 1.5, zc - 0.9), (fx, y + 1.5, zc + 0.9))
            m.quad("M_Glass", (fx + 0.03, y, zc + 0.72), (fx + 0.03, y, zc - 0.72),
                   (fx + 0.03, y + 1.32, zc - 0.72), (fx + 0.03, y + 1.32, zc + 0.72))
    return m


def build_roof():
    """屋面 + 白色女儿墙 + 屋顶柱廊 + 出屋面楼梯间 + 设备"""
    m = Mesh()
    y = H
    # 屋面
    m.box("M_Roof", X0 - 0.06, y, Z0 - 0.06, X1 + 0.06, y + 0.14, Z1 + 0.06)

    # 女儿墙
    t = 0.24
    ph = y + PARAPET
    m.box("M_Wall_White", X0, y, Z0 - 0.06, X1, ph, Z0 - 0.06 + t)
    m.box("M_Wall_White", X0, y, Z1 + 0.06 - t, X1, ph, Z1 + 0.06)
    m.box("M_Wall_White", X0, y, Z0, X0 + t, ph, Z1)
    m.box("M_Wall_White", X1 - t, y, Z0, X1, ph, Z1)

    # 屋顶白色柱廊（照片里屋檐上那圈方柱 + 压顶梁，本楼最抢眼的特征）
    col_top = ph + COL_H
    step = 2.05
    n = max(2, int(L / step))
    for i in range(n + 1):
        px = X0 + t / 2 + i * (L - t) / n
        for zc in (Z0 - 0.06 + t / 2, Z1 + 0.06 - t / 2):
            m.box("M_Wall_White", px - 0.17, ph - 0.06, zc - 0.17, px + 0.17, col_top, zc + 0.17)
    nz = max(2, int(D / step))
    for i in range(nz + 1):
        pz = Z0 + t / 2 + i * (D - t) / nz
        for xc in (X0 + t / 2, X1 - t / 2):
            m.box("M_Wall_White", xc - 0.17, ph - 0.06, pz - 0.17, xc + 0.17, col_top, pz + 0.17)

    # 柱廊压顶梁（四面一圈）
    bh = col_top + 0.20
    m.box("M_Wall_White", X0 - 0.10, col_top, Z0 - 0.16, X1 + 0.10, bh, Z0 + 0.22)
    m.box("M_Wall_White", X0 - 0.10, col_top, Z1 - 0.22, X1 + 0.10, bh, Z1 + 0.16)
    m.box("M_Wall_White", X0 - 0.10, col_top, Z0, X0 + 0.22, bh, Z1)
    m.box("M_Wall_White", X1 - 0.22, col_top, Z0, X1 + 0.10, bh, Z1)

    # 出屋面楼梯间
    m.box("M_Brick", -13.0, y, 1.6, -7.4, y + 2.9, 6.2)
    m.box("M_Frame", -13.2, y + 2.9, 1.4, -7.2, y + 3.12, 6.4)
    # 设备基础 / 空调外机
    m.box("M_Equip", 9.0, y + 0.14, -5.4, 12.6, y + 1.9, -3.0)
    m.box("M_Equip", 15.2, y + 0.14, -5.0, 17.4, y + 1.5, -3.4)
    m.box("M_Equip", -24.0, y + 0.14, -5.2, -20.8, y + 1.7, -3.2)
    return m


# ----------------------------------------------------------------------------
# 周边建筑（半透明体块）
# ----------------------------------------------------------------------------
NEIGHBORS = [
    # (cx, cz, w, d, h)
    (-66, -74, 62, 15.0, 21.0),
    (2, -78, 58, 15.0, 21.0),
    (68, -72, 54, 15.0, 18.2),
    (-62, 76, 58, 15.0, 21.0),
    (6, 80, 62, 15.0, 21.0),
    (72, 75, 56, 15.0, 18.2),
    (-124, 4, 22.0, 46.0, 15.4),
    (118, -8, 24.0, 44.0, 16.2),
    (-8, -116, 72.0, 27.0, 12.6),
    (64, 112, 46.0, 30.0, 14.2),
    (-72, 114, 40.0, 26.0, 12.0),
    (12, 132, 84.0, 22.0, 18.0),
    (-140, 120, 34.0, 34.0, 10.5),
    (146, 60, 30.0, 30.0, 12.0),
]


def build_neighbors():
    nodes = []
    for i, (cx, cz, w, d, h) in enumerate(NEIGHBORS):
        m = Mesh()
        m.box("M_Neighbor", cx - w / 2, 0.0, cz - d / 2, cx + w / 2, h, cz + d / 2)
        # 顶部薄檐，让体积感更清楚
        m.box("M_Neighbor", cx - w / 2 - 0.3, h, cz - d / 2 - 0.3,
              cx + w / 2 + 0.3, h + 0.9, cz + d / 2 + 0.3)
        nodes.append(("Neighbor_%02d" % (i + 1), m))
    return nodes


def build_site():
    """地面 + 道路"""
    m = Mesh()
    # 大地
    m.quad_y("M_Ground", -300, -300, 300, 300, 0.0)
    # 两条主路
    m.quad_y("M_Road", -260, 34.0, 260, 43.0, 0.02)
    m.quad_y("M_Road", 96.0, -260, 105.0, 260, 0.02)
    # 楼前道路与场地铺装
    m.quad_y("M_Road", -300, 24.0, 300, 31.0, 0.02)
    m.quad_y("M_Road", X0 - 1, -12.6, X1 + 1, -7.6, 0.03)
    m.quad_y("M_Road", X0 - 1, 7.6, X1 + 1, 12.6, 0.03)
    return m


# ----------------------------------------------------------------------------
# GLB 写出
# ----------------------------------------------------------------------------
def s2l(c):
    """sRGB -> 线性。glTF 的 baseColorFactor 定义在线性空间，
    这里按人眼观感取值后再转换，避免渲染出来发白发粉。"""
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def write_glb(path, nodes):
    blob = bytearray()
    accessors, buffer_views, meshes, node_defs = [], [], [], []

    def push(data, target):
        while len(blob) % 4:
            blob.append(0)
        off = len(blob)
        blob.extend(data)
        buffer_views.append({"buffer": 0, "byteOffset": off,
                             "byteLength": len(data), "target": target})
        return len(buffer_views) - 1

    for name, mesh in nodes:
        prims = []
        for mat_name, (pos, nor, idx) in mesh.buckets.items():
            if not idx:
                continue
            fb = struct.pack("<%df" % (len(pos) * 3), *[c for p in pos for c in p])
            nb = struct.pack("<%df" % (len(nor) * 3), *[c for n in nor for c in n])
            ib = struct.pack("<%dI" % len(idx), *idx)
            bv_p, bv_n, bv_i = push(fb, 34962), push(nb, 34962), push(ib, 34963)
            mn = [min(p[i] for p in pos) for i in range(3)]
            mx = [max(p[i] for p in pos) for i in range(3)]
            accessors.append({"bufferView": bv_p, "componentType": 5126, "count": len(pos),
                              "type": "VEC3", "min": mn, "max": mx})
            ai_p = len(accessors) - 1
            accessors.append({"bufferView": bv_n, "componentType": 5126, "count": len(nor),
                              "type": "VEC3"})
            ai_n = len(accessors) - 1
            accessors.append({"bufferView": bv_i, "componentType": 5125, "count": len(idx),
                              "type": "SCALAR"})
            ai_i = len(accessors) - 1
            prims.append({"attributes": {"POSITION": ai_p, "NORMAL": ai_n},
                          "indices": ai_i, "material": MAT_INDEX[mat_name], "mode": 4})
        if not prims:
            continue
        meshes.append({"name": name + "_mesh", "primitives": prims})
        node_defs.append({"name": name, "mesh": len(meshes) - 1})

    materials = []
    for nm, col, met, rgh, mode in MATERIALS:
        lin = (s2l(col[0]), s2l(col[1]), s2l(col[2]), col[3])
        d = {"name": nm, "doubleSided": False,
             "pbrMetallicRoughness": {"baseColorFactor": list(lin),
                                      "metallicFactor": met,
                                      "roughnessFactor": rgh}}
        if mode != "OPAQUE":
            d["alphaMode"] = mode
        materials.append(d)

    gltf = {
        "asset": {"version": "2.0", "generator": "UniHub campus modeler (pure python)"},
        "scene": 0,
        "scenes": [{"name": "Campus", "nodes": list(range(len(node_defs)))}],
        "nodes": node_defs,
        "meshes": meshes,
        "materials": materials,
        "accessors": accessors,
        "bufferViews": buffer_views,
        "buffers": [{"byteLength": len(blob)}],
    }
    js = json.dumps(gltf, separators=(",", ":")).encode("utf-8")
    while len(js) % 4:
        js += b" "
    while len(blob) % 4:
        blob.append(0)
    total = 12 + 8 + len(js) + 8 + len(blob)
    with open(path, "wb") as f:
        f.write(b"glTF")
        f.write(struct.pack("<II", 2, total))
        f.write(struct.pack("<II", len(js), 0x4E4F534A))
        f.write(js)
        f.write(struct.pack("<II", len(blob), 0x004E4942))
        f.write(blob)
    return total


def main():
    os.makedirs(OUT, exist_ok=True)
    nodes = []
    for k in range(1, NFL + 1):
        nodes.append(("Floor_%d" % k, build_floor(k)))
    nodes.append(("Core_W", build_core("W")))
    nodes.append(("Core_E", build_core("E")))
    nodes.append(("Roof", build_roof()))
    nodes.append(("Site", build_site()))
    nodes += build_neighbors()

    p = os.path.join(OUT, "campus.glb")
    size = write_glb(p, nodes)
    tris = 0
    for _, m in nodes:
        for _, (_, _, idx) in m.buckets.items():
            tris += len(idx) // 3
    print("wrote %s  %.1f KB  nodes=%d  tris=%d" % (p, size / 1024, len(nodes), tris))


if __name__ == "__main__":
    main()
