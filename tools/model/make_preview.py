# -*- coding: utf-8 -*-
"""把 three.js / OrbitControls / GLTFLoader / GLB 全部内联进单个 html"""
import base64
import os

HERE = os.path.dirname(os.path.abspath(__file__))
V = os.path.join(HERE, "vendor")


def read(p):
    with open(p, "r", encoding="utf-8") as f:
        return f.read()


def main():
    tpl = read(os.path.join(HERE, "preview_template.html"))
    with open(os.path.join(HERE, "out", "campus.glb"), "rb") as f:
        b64 = base64.b64encode(f.read()).decode("ascii")

    import re
    tpl = re.sub(r'\s+data-page-node-id="[^"]*"', '', tpl)
    out = (tpl
           .replace("/*__THREE__*/", read(os.path.join(V, "three.min.js")))
           .replace("/*__ORBIT__*/", read(os.path.join(V, "OrbitControls.js")))
           .replace("/*__GLTF__*/", read(os.path.join(V, "GLTFLoader.js")))
           .replace("__GLB_B64__", b64))

    p = os.path.join(HERE, "out", "campus_preview.html")
    with open(p, "w", encoding="utf-8") as f:
        f.write(out)
    print("wrote %s  %.1f KB" % (p, os.path.getsize(p) / 1024))


if __name__ == "__main__":
    main()
