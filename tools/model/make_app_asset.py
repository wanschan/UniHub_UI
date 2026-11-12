# -*- coding: utf-8 -*-
"""把模型打成 App 可直接用的单文件页面，输出到 app/src/main/assets/map/campus.html

    python make_app_asset.py
"""
import base64
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
VENDOR = os.path.join(HERE, "vendor")
PROJ = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSET_DIR = os.path.join(PROJ, "app", "src", "main", "assets", "map")


def read(p):
    with open(p, "r", encoding="utf-8") as f:
        return f.read()


def main():
    tpl = read(os.path.join(HERE, "app_template.html"))
    tpl = re.sub(r'\s+data-page-node-id="[^"]*"', "", tpl)

    with open(os.path.join(HERE, "out", "campus.glb"), "rb") as f:
        b64 = base64.b64encode(f.read()).decode("ascii")

    html = (tpl
            .replace("/*__THREE__*/", read(os.path.join(VENDOR, "three.min.js")))
            .replace("/*__ORBIT__*/", read(os.path.join(VENDOR, "OrbitControls.js")))
            .replace("/*__GLTF__*/", read(os.path.join(VENDOR, "GLTFLoader.js")))
            .replace("__GLB_B64__", b64))

    os.makedirs(ASSET_DIR, exist_ok=True)
    out = os.path.join(ASSET_DIR, "campus.html")
    with open(out, "w", encoding="utf-8", newline="\n") as f:
        f.write(html)
    print("wrote %s  %.0f KB" % (out, os.path.getsize(out) / 1024))


if __name__ == "__main__":
    main()
