#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""把用户提供的 UniHub 图标（白底深蓝）处理成 Android 各密度图标资源。"""
import os
from PIL import Image, ImageDraw

SRC = r"C:\Users\DELL\.workbuddy\clipboard-images\clipboard-2026-11-12T00-17-09-897Z-0fce324e.jpg"
RES = r"C:\Users\DELL\AndroidStudioProjects\applicationOneApp\app\src\main\res"
OUT = r"C:\Users\DELL\AndroidStudioProjects\applicationOneApp\build\icon-preview"
NAVY = (44, 60, 76)

def cut_white(im):
    """白底 -> 透明，保留原色。"""
    im = im.convert("RGBA")
    w, h = im.size
    px = im.load()
    for y in range(h):
        for x in range(w):
            r, g, b, _ = px[x, y]
            l = 0.299 * r + 0.587 * g + 0.114 * b
            if l >= 246:
                a = 0
            elif l <= 231:
                a = 255
            else:
                a = int((246 - l) / 15.0 * 255)
            px[x, y] = (r, g, b, a)
    bbox = im.split()[3].point(lambda v: 255 if v > 12 else 0).getbbox()
    return im.crop(bbox)

def art_for(art, box, ratio, canvas, bg=None, circle=False):
    """把 art 按 max 边 = ratio*canvas 居中贴到 canvas 正方形上。"""
    c = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    d = ImageDraw.Draw(c)
    if bg:
        if circle:
            d.ellipse([0, 0, canvas - 1, canvas - 1], fill=bg)
        else:
            d.rectangle([0, 0, canvas, canvas], fill=bg)
    target = max(1, int(canvas * ratio))
    aw, ah = art.size
    k = target / max(aw, ah)
    nw, nh = max(1, round(aw * k)), max(1, round(ah * k))
    a = art.resize((nw, nh), Image.LANCZOS)
    c.alpha_composite(a, ((canvas - nw) // 2, (canvas - nh) // 2))
    return c

def save(img, folder, name, size):
    img = img.resize((size, size), Image.LANCZOS)
    d = os.path.join(RES, folder)
    os.makedirs(d, exist_ok=True)
    p = os.path.join(d, name)
    img.save(p, "PNG", optimize=True)
    return p

def main():
    src = Image.open(SRC)
    art = cut_white(src)
    print("trimmed art:", art.size)

    DENS = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}

    # 1) 传统 launcher 图标（API < 26）
    for d, s in DENS.items():
        n = int(48 * s)
        save(art_for(art, None, 0.86, n * 4, bg=(255, 255, 255)), "mipmap-" + d, "ic_launcher.png", n)
        save(art_for(art, None, 0.64, n * 4, bg=(255, 255, 255), circle=True), "mipmap-" + d, "ic_launcher_round.png", n)

    # 2) 自适应图标前景（108dp 画布，内容压在中间 66dp 安全区）
    for d, s in DENS.items():
        n = int(108 * s)
        save(art_for(art, None, 0.60, n * 4), "drawable-" + d, "ic_launcher_foreground.png", n)

    # 3) Android 12+ 系统开屏图标：白盘 + Logo
    for d, s in DENS.items():
        n = int(108 * s)
        save(art_for(art, None, 0.50, n * 4, bg=(255, 255, 255), circle=True), "drawable-" + d, "ic_splash_mark.png", n)

    # 4) 自绘开屏圆盘里的 Logo
    for d, s in DENS.items():
        n = int(108 * s)
        save(art_for(art, None, 0.62, n * 4), "drawable-" + d, "ic_logo_mark.png", n)

    # ---------------- 预览图
    os.makedirs(OUT, exist_ok=True)
    S = 256
    sheet = Image.new("RGB", (S * 4 + 120, S + 96), (245, 246, 248))
    tiles = []
    tiles.append(("圆形遮罩", art_for(art, None, 0.60, S, bg=(255, 255, 255), circle=True)))
    # squircle
    sq = art_for(art, None, 0.60, S, bg=(255, 255, 255))
    mask = Image.new("L", (S * 4, S * 4), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, S * 4 - 1, S * 4 - 1], radius=int(S * 4 * 0.22), fill=255)
    sq.putalpha(mask.resize((S, S), Image.LANCZOS))
    tiles.append(("圆角遮罩", sq))
    tiles.append(("传统图标", art_for(art, None, 0.86, S, bg=(255, 255, 255))))
    # 开屏：深底 + 白盘
    sp = Image.new("RGB", (S, S), (17, 18, 20))
    sp.paste(art_for(art, None, 0.36, S, bg=(255, 255, 255), circle=True), (0, 0), art_for(art, None, 0.36, S, bg=(255, 255, 255), circle=True))
    tiles.append(("系统开屏", sp))
    for i, (label, img) in enumerate(tiles):
        x = 30 + i * (S + 20)
        sheet.paste(img, (x, 40), img if img.mode == "RGBA" else None)
    sheet.save(os.path.join(OUT, "icons.png"))
    print("preview:", os.path.join(OUT, "icons.png"))

if __name__ == "__main__":
    main()
