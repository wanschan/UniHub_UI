# -*- coding: utf-8 -*-
"""
在 Blender 里重建 UniHub 校园楼栋模型（男生宿舍 6F）

用法一（命令行生成 .blend）：
    "D:/Blender 4.3/blender.exe" -b -P build_campus.py

用法二（在 Blender 里改参数重跑）：
    打开 campus.blend  →  Text Editor  →  选中 build_campus.py  →  Run Script

几何全部来自同目录的 gen_campus.py（与导出的 GLB 完全一致），
本脚本只负责把它变成 Blender 里可编辑的对象、材质、灯光和相机。

坐标：模型是 Y-up，Blender 是 Z-up，转换 (x, y, z) -> (x, -z, y)。
"""
import os
import sys
import math

import bpy
from mathutils import Vector

# ---------------------------------------------------------------- 参数
PARAMS = dict(
    bays=15,            # 开间数
    bay=4.4,            # 开间宽 (m)
    depth=15.0,         # 进深 (m)
    floors=6,           # 层数
    h_first=3.9,        # 一层层高
    h_floor=3.3,        # 标准层高
    win_w=2.55,
    win_h=1.55,
    entrance_bay=3,
    back_door_bay=10,
)

BUILD_NEIGHBORS = True      # 是否生成周边半透明体块
RESET_SCENE = True          # 重建前清空场景
SAVE_BLEND = ""             # 留空 = 保存到 out/campus.blend

# ---------------------------------------------------------------- 路径
try:
    HERE = os.path.dirname(os.path.abspath(__file__))
except NameError:                       # 从 Text Editor 运行时没有 __file__
    HERE = bpy.path.abspath("//blender") if bpy.data.filepath else ""

MODEL_DIR = os.path.dirname(HERE.rstrip("/\\")) if HERE else ""
for p in (HERE, MODEL_DIR):
    if p and p not in sys.path:
        sys.path.insert(0, p)

import gen_campus as G
import importlib
importlib.reload(G)

OUT_DIR = os.path.join(MODEL_DIR or HERE, "out")


# ---------------------------------------------------------------- 工具
def s2l(c):
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def to_blender(p):
    """模型坐标 (Y-up) -> Blender 坐标 (Z-up)"""
    return (p[0], -p[2], p[1])


def clear_scene():
    if RESET_SCENE:
        bpy.ops.wm.read_factory_settings(use_empty=True)
    else:
        for ob in list(bpy.data.objects):
            bpy.data.objects.remove(ob, do_unlink=True)


def get_collection(name):
    col = bpy.data.collections.get(name)
    if col is None:
        col = bpy.data.collections.new(name)
        bpy.context.scene.collection.children.link(col)
    return col


# ---------------------------------------------------------------- 材质
def build_materials():
    for name, col, met, rgh, mode in G.MATERIALS:
        mat = bpy.data.materials.get(name) or bpy.data.materials.new(name)
        mat.use_nodes = True
        nt = mat.node_tree
        bsdf = nt.nodes.get("Principled BSDF")
        if bsdf is None:
            bsdf = nt.nodes.new("ShaderNodeBsdfPrincipled")
            nt.links.new(bsdf.outputs[0], nt.nodes["Material Output"].inputs[0])

        rgba = (s2l(col[0]), s2l(col[1]), s2l(col[2]), col[3])
        bsdf.inputs["Base Color"].default_value = rgba
        bsdf.inputs["Metallic"].default_value = met
        bsdf.inputs["Roughness"].default_value = rgh
        if "Alpha" in bsdf.inputs:
            bsdf.inputs["Alpha"].default_value = col[3]

        if mode != "OPAQUE":
            for attr, val in (("blend_method", "BLEND"),
                              ("surface_render_method", "BLENDED")):
                try:
                    setattr(mat, attr, val)
                except (AttributeError, TypeError):
                    pass
            mat.use_backface_culling = False
        mat.diffuse_color = rgba          # 实体视图配色
        mat.roughness = rgh


# ---------------------------------------------------------------- 几何
def mesh_to_object(name, mesh, collection):
    """把一个 gen_campus.Mesh 变成带多材质槽的 Blender 物体"""
    verts, faces, face_mat = [], [], []
    mats = []

    for mat_name, (pos, _nor, idx) in mesh.buckets.items():
        if not idx:
            continue
        mi = len(mats)
        mats.append(mat_name)
        base = len(verts)
        verts.extend(to_blender(p) for p in pos)
        # idx 是按 每 6 个一组（两个三角形 = 一个四边形）写入的
        for k in range(0, len(idx), 6):
            a, b, c, d = idx[k], idx[k + 1], idx[k + 2], idx[k + 5]
            faces.append((base + a, base + b, base + c, base + d))
            face_mat.append(mi)

    if not faces:
        return None

    me = bpy.data.meshes.new(name)
    me.from_pydata(verts, [], faces)
    me.validate(verbose=False)

    for mn in mats:
        me.materials.append(bpy.data.materials[mn])

    if len(me.polygons) == len(face_mat):
        for i, poly in enumerate(me.polygons):
            poly.material_index = face_mat[i]
            poly.use_smooth = False
    else:
        print("[warn] %s 面数不匹配 %d vs %d" % (name, len(me.polygons), len(face_mat)))

    me.update()
    ob = bpy.data.objects.new(name, me)
    collection.objects.link(ob)
    ob.select_set(False)
    return ob


def build_all():
    G.configure(**PARAMS)

    col_main = get_collection("主体楼")
    col_hood = get_collection("周边建筑")
    col_site = get_collection("场地")

    objs = []
    # 逐层
    for k in range(1, G.NFL + 1):
        ob = mesh_to_object("Floor_%d" % k, G.build_floor(k), col_main)
        if ob:
            ob["floor"] = k
            ob["floor_z"] = G.floor_base(k)
            objs.append(ob)

    for nm, mesh in (("Core_W", G.build_core("W")), ("Core_E", G.build_core("E")),
                     ("Roof", G.build_roof())):
        ob = mesh_to_object(nm, mesh, col_main)
        if ob:
            objs.append(ob)

    ob = mesh_to_object("Site", G.build_site(), col_site)
    if ob:
        objs.append(ob)

    if BUILD_NEIGHBORS:
        for nm, mesh in G.build_neighbors():
            ob = mesh_to_object(nm, mesh, col_hood)
            if ob:
                objs.append(ob)

    return objs


# ---------------------------------------------------------------- 场景
def build_world():
    scene = bpy.context.scene
    world = bpy.data.worlds.get("Campus Sky") or bpy.data.worlds.new("Campus Sky")
    scene.world = world
    world.use_nodes = True
    nt = world.node_tree
    nt.nodes.clear()

    out = nt.nodes.new("ShaderNodeOutputWorld")
    bg = nt.nodes.new("ShaderNodeBackground")
    ramp = nt.nodes.new("ShaderNodeValToRGB")
    mapr = nt.nodes.new("ShaderNodeMapRange")
    sep = nt.nodes.new("ShaderNodeSeparateXYZ")
    tc = nt.nodes.new("ShaderNodeTexCoord")

    mapr.inputs["From Min"].default_value = -1.0
    mapr.inputs["From Max"].default_value = 1.0
    # 天顶 -> 地平线
    ramp.color_ramp.elements[0].position = 0.42
    ramp.color_ramp.elements[0].color = (0.757, 0.835, 0.882, 1)   # 地平线灰蓝
    ramp.color_ramp.elements[1].position = 0.92
    ramp.color_ramp.elements[1].color = (0.361, 0.545, 0.729, 1)   # 天顶

    nt.links.new(tc.outputs["Generated"], sep.inputs["Vector"])
    nt.links.new(sep.outputs["Z"], mapr.inputs["Value"])
    nt.links.new(mapr.outputs["Result"], ramp.inputs["Fac"])
    nt.links.new(ramp.outputs["Color"], bg.inputs["Color"])
    nt.links.new(bg.outputs[0], out.inputs["Surface"])
    bg.inputs["Strength"].default_value = 1.0


def add_sun(pos=(118, 168, 84), target=(0, 10, 0), energy=4.0, color=(1, 0.957, 0.886)):
    light = bpy.data.lights.get("Sun") or bpy.data.lights.new("Sun", type='SUN')
    light.energy = energy
    light.color = color
    light.angle = math.radians(2.2)
    ob = bpy.data.objects.get("Sun") or bpy.data.objects.new("Sun", light)
    if ob.name not in bpy.context.scene.collection.objects:
        try:
            bpy.context.scene.collection.objects.link(ob)
        except RuntimeError:
            pass
    ob.location = to_blender(pos)
    d = Vector(to_blender(target)) - Vector(to_blender(pos))
    ob.rotation_euler = d.to_track_quat('-Z', 'Y').to_euler()

    fill = bpy.data.lights.new("Fill", type='AREA')
    fill.energy = 22000
    fill.size = 120
    fill.color = (0.62, 0.75, 0.9)
    fob = bpy.data.objects.new("Fill", fill)
    bpy.context.scene.collection.objects.link(fob)
    fpos = (-140, 70, -120)
    fob.location = to_blender(fpos)
    fd = Vector(to_blender((0, 12, 1))) - Vector(to_blender(fpos))
    fob.rotation_euler = fd.to_track_quat('-Z', 'Y').to_euler()
    return ob


def add_cameras():
    cams = [
        ("Cam_鸟瞰", (74, 43, 82), (0, 10, 0), 42),
        ("Cam_人视", (70, 1.85, 64), (1, 9.0, 0), 52),
        ("Cam_正立面", (0, 10.5, 68), (0, 10.5, 0), 56),
    ]
    made = []
    for name, pos, tgt, fov in cams:
        cam = bpy.data.cameras.new(name)
        cam.sensor_width = 36
        cam.lens = 36.0 / (2.0 * math.tan(math.radians(fov) / 2.0))
        cam.clip_start = 0.1
        cam.clip_end = 3000
        ob = bpy.data.objects.new(name, cam)
        bpy.context.scene.collection.objects.link(ob)
        ob.location = to_blender(pos)
        d = Vector(to_blender(tgt)) - Vector(to_blender(pos))
        ob.rotation_euler = d.to_track_quat('-Z', 'Y').to_euler()
        made.append(ob)

    bpy.context.scene.camera = made[0]
    return made


def setup_render():
    scene = bpy.context.scene
    for eng in ("BLENDER_EEVEE_NEXT", "BLENDER_EEVEE", "CYCLES"):
        try:
            scene.render.engine = eng
            break
        except TypeError:
            continue
    scene.render.resolution_x = 1280
    scene.render.resolution_y = 760
    scene.render.resolution_percentage = 100
    scene.render.film_transparent = False
    try:
        scene.view_settings.view_transform = 'Standard'
    except TypeError:
        pass
    try:
        scene.eevee.taa_render_samples = 64
        scene.eevee.use_raytracing = True
    except AttributeError:
        pass
    scene.unit_settings.system = 'METRIC'
    scene.unit_settings.scale_length = 1.0


# ---------------------------------------------------------------- 说明
README = u"""UniHub · 男生宿舍 6F —— Blender 场景说明

【场景结构】
  主体楼/     Floor_1 … Floor_6（逐层，可单独选中/隐藏/移动）、Core_W、Core_E、Roof
  周边建筑/   Neighbor_01 … Neighbor_14（半透明体块，整组在「大纲视图」里可以一键隐藏）
  场地/       Site（地面、道路、楼前铺装）

【常用操作】
  · 数量改层数/开间：打开本文件同目录的 build_campus.py，改顶部 PARAMS，Run Script 重建。
  · 只想要建筑不要周边：把 BUILD_NEIGHBORS 改 False。
  · 相机：Cam_鸟瞰 / Cam_人视 / Cam_正立面，渲染 F12 即可出图。
  · 周边体块想更透明：选中 Neighbor_* 的 M_Neighbor 材质，调 Principled BSDF 的 Alpha。

【注意】
  · 模型单位是米，地面在 z=0，楼体长轴沿 X。
  · glTF 用的是线性色，本文件里的材质已经按线性空间设好，改颜色时用取色器即可。
  · 几何是扁平四边形（无平滑），按材质分槽，进入编辑模式后可按材质选择整批窗、墙。
"""


def embed_texts():
    for name, body in (("build_campus.py", None), ("README_场景说明.txt", README)):
        txt = bpy.data.texts.get(name)
        if txt is None:
            txt = bpy.data.texts.new(name)
        if body is None:
            try:
                with open(os.path.abspath(__file__), "r", encoding="utf-8") as f:
                    body = f.read()
            except (NameError, OSError):
                body = "# 无法读取源文件\n"
        txt.clear()
        txt.write(body)


# ---------------------------------------------------------------- 主流程
def main():
    clear_scene()
    build_materials()
    objs = build_all()
    build_world()
    add_sun()
    if bpy.app.background:
        add_cameras()
    else:
        add_cameras()
    setup_render()
    embed_texts()

    path = SAVE_BLEND or os.path.join(OUT_DIR, "campus.blend")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=path)

    print("[campus] objects=%d  ->  %s" % (len(objs), path))


if __name__ == "__main__":
    main()
