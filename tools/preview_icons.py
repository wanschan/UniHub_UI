#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""生成图标在真实场景下的效果预览（圆形/圆角遮罩、系统开屏、自绘开屏白盘）。"""
import os
from PIL import Image, ImageDraw

RES = r"C:\Users\DELL\AndroidStudioProjects\applicationOneApp\app\src\main\res"
OUT = r"C:\Users\DELL\AndroidStudioProjects\applicationOneApp\build\icon-preview\final.png"

fg = Image.open(os.path.join(RES, "drawable-xxxhdpi", "ic_launcher_foreground.png")).convert("RGBA")
splash = Image.open(os.path.join(RES, "drawable-xxxhdpi", "ic_splash_mark.png")).convert("RGBA")
logo = Image.open(os.path.join(RES, "drawable-xxxhdpi", "ic_logo_mark.png")).convert("RGBA")

T = 300            # 每个面板边长
GAP = 24
SS = 4             # 超采样


def panel(bg):
    img = Image.new("RGB", (T * SS, T * SS), bg)
    return img


def adaptive(size, radius_ratio=None, circle=False):
    """把前景叠在白底上，再按遮罩裁切。"""
    s = int(size * SS)
    layer = Image.new("RGBA", (s, s), (255, 255, 255, 255))
    art = fg.resize((s, s), Image.LANCZOS)
    layer.alpha_composite(art)
    mask = Image.new("L", (s, s), 0)
    d = ImageDraw.Draw(mask)
    if circle:
        d.ellipse([0, 0, s - 1, s - 1], fill=255)
    else:
        d.rounded_rectangle([0, 0, s - 1, s - 1], radius=int(s * (radius_ratio or 0.22)), fill=255)
    layer.putalpha(Image.composite(mask, Image.new("L", (s, s), 0), layer.split()[3]))
    return layer


def paste_center(base, img, scale=0.72):
    s = int(T * SS * scale)
    im = img.resize((s, s), Image.LANCZOS)
    base.paste(im, ((base.width - s) // 2, (base.height - s) // 2), im)


# 1 浅色桌面 + 圆形遮罩
p1 = panel((236, 238, 242))
paste_center(p1, adaptive(T, circle=True), 0.74)
# 2 深色桌面 + 圆角方形遮罩
p2 = panel((32, 34, 39))
paste_center(p2, adaptive(T, radius_ratio=0.24), 0.74)

# 3 系统开屏：深底 + 遮罩后的白盘（系统遮罩 2/3）
p3 = panel((21, 22, 26))
p3.alpha = None
disc = splash.resize((int(T * SS * 0.74),) * 2, Image.LANCZOS)
p3.paste(disc, ((p3.width - disc.width) // 2, (p3.height - disc.height) // 2), disc)

# 4 自绘开屏：112dp 白盘 + 105dp logo 盒
p4 = panel((21, 22, 26))
d4 = ImageDraw.Draw(p4)
dr = int(T * SS * 0.72)
box = int(dr * 0.94)
d4.ellipse([(T * SS - dr) // 2, (T * SS - dr) // 2, (T * SS + dr) // 2, (T * SS + dr) // 2], fill=(255, 255, 255))
lg = logo.resize((box, box), Image.LANCZOS)
p4.paste(lg, ((T * SS - box) // 2, (T * SS - box) // 2), lg)

panels = [p1, p2, p3, p4]
sheet = Image.new("RGB", (T * 4 + GAP * 5, T + GAP * 2), (18, 19, 22))
for i, p in enumerate(panels):
    sheet.paste(p.resize((T, T), Image.LANCZOS), (GAP + i * (T + GAP), GAP))
sheet.save(OUT)
print(OUT, sheet.size)
