# -*- coding: utf-8 -*-
"""渲染 .blend 里的三个相机（命令行用）
    "D:/Blender 4.3/blender.exe" -b out/campus.blend -P render_views.py
"""
import os
import sys

import bpy

OUT = os.path.join(os.path.dirname(bpy.data.filepath), "blender_render")
os.makedirs(OUT, exist_ok=True)

scene = bpy.context.scene
cams = [o for o in bpy.data.objects if o.type == 'CAMERA']

for cam in cams:
    scene.camera = cam
    scene.render.filepath = os.path.join(OUT, cam.name.replace("Cam_", "") + ".png")
    bpy.ops.render.render(write_still=True)
    print("[render]", scene.render.filepath)
